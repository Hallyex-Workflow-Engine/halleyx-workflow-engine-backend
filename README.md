# Halleyx Workflow Engine - Backend

A production-ready business process automation engine built with Spring Boot. This service provides RESTful APIs for designing workflows, managing multi-step approval chains, executing processes, and tracking real-time execution state across role-based users.

---

## Project Demo

[![Halleyx Workflow Engine Demo](https://img.youtube.com/vi/ucqeoIy4o6c/maxresdefault.jpg)](https://youtu.be/ucqeoIy4o6c)

Click the thumbnail above to watch the full project walkthrough on YouTube.

---
open the react on the port  = "http://localhost:5173"

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Role-Based Access](#role-based-access)
- [System Flow](#system-flow)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Rule Engine](#rule-engine)
- [Email Notification](#email-notification)
- [Installation Guide](#installation-guide)
- [Configuration](#configuration)
- [Testing](#testing)

---

## Overview

The Workflow Engine backend is responsible for:

- Authenticating users and enforcing role-based access control
- Persisting workflow definitions, steps, and decision rules
- Executing workflows by evaluating MVEL-based conditional rules
- Managing approval lifecycles with real-time state transitions
- Sending email notifications to finance teams via Spring Mail
- Providing a full audit trail of all execution events

---

## Architecture

```
Client (React)
      |
      | HTTP / REST / JSON
      |
Spring Boot Application
      |
      +-- SecurityConfig (JWT-less session-based auth)
      |
      +-- Controllers (Auth, Workflow, Step, Rule, Execution, User)
      |
      +-- Services (WorkflowService, ExecutionService, RuleEngine)
      |
      +-- Repositories (JPA + MySQL)
      |
      +-- MVEL Rule Engine (condition evaluation)
      |
      +-- Spring Mail (finance team notifications)
      |
MySQL Database
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.x |
| Language | Java 17 |
| Security | Spring Security 6 (session-based) |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Rule Engine | MVEL 2.x |
| Email | Spring Boot Mail Starter |
| Build Tool | Maven |
| Mapping | ModelMapper |
| Password | BCrypt |

---

## Role-Based Access

The system supports four distinct roles. Each role maps to a dedicated frontend dashboard and backend permission set.

| Role | Description | Permissions |
|---|---|---|
| ADMIN | System administrator | Create, edit, delete workflows, steps, rules. View full audit log. Manage users. |
| MANAGER | Middle-level approver | View and act on pending approvals assigned to their email. View approval history. |
| CEO | Senior approver | Same as Manager plus visibility into all executions and all workflow steps. |
| EMPLOYEE | End user | Execute workflows by submitting input data. Track progress of their own submissions. |

---

## System Flow

The following describes the complete lifecycle of a workflow execution.

```
ADMIN creates workflow
  |
  +-- Defines input schema (field names, types, required flags)
  |
  +-- Adds steps in order:
  |     Step 1: Manager Approval     (APPROVAL      - assignee: manager@company.com)
  |     Step 2: Finance Notification (NOTIFICATION  - auto-send email)
  |     Step 3: CEO Approval         (APPROVAL      - assignee: ceo@company.com)
  |     Step 4: Task Completion      (TASK          - auto-complete)
  |
  +-- Adds rules per step (MVEL conditions with next step routing):
        amount > 5000 && priority == 'High'  --> CEO Approval
        amount <= 5000                        --> Finance Notification
        DEFAULT                               --> Task Rejection

EMPLOYEE submits execution with input data
  |
  +-- ExecutionService.startExecution()
  |     - Validates workflow is active and has a start step
  |     - Creates Execution record with status PENDING
  |     - Calls processStep()
  |
  +-- processStep() logic:
        If APPROVAL step  --> save state as IN_PROGRESS, stop and wait
        If NOTIFICATION   --> send email, log step, evaluate rules, move to next step
        If TASK           --> log step, evaluate rules, move to next step or COMPLETE/FAIL

MANAGER or CEO receives pending approval in their dashboard
  |
  +-- approveStep()
  |     - Logs approval with approver ID and timestamp
  |     - Evaluates rules on current step
  |     - Moves to next step and calls processStep() recursively
  |
  +-- rejectStep()
        - Logs rejection with comment
        - Sets execution status to FAILED immediately

Execution reaches final step with no next step --> COMPLETED
Execution hits rejection step name             --> FAILED
```

---

## Project Structure

```
src/main/java/com/halleyx/workflow_engine/
|
+-- config/
|     SecurityConfig.java           - CORS, session auth, route-level role guards
|
+-- controller/
|     AuthController.java           - /api/auth/**
|     WorkflowController.java       - /api/workflows/**
|     StepController.java           - /api/steps/**, /api/workflows/{id}/steps
|     RuleController.java           - /api/rules/**, /api/steps/{id}/rules
|     ExecutionController.java      - /api/executions/**, /api/workflows/{id}/execute
|     UserController.java           - /api/users/**
|
+-- service/
|     WorkflowService.java          - Workflow CRUD, versioning, step counting
|     StepService.java              - Step CRUD
|     RuleService.java              - Rule CRUD
|     ExecutionService.java         - Core execution engine, approval, rejection
|     CustomUserDetailsService.java - Spring Security user loading
|
+-- engine/
|     RuleEngine.java               - MVEL condition evaluator, priority-based routing
|
+-- entity/
|     Workflow.java
|     Step.java
|     Rule.java
|     Execution.java
|     User.java
|     Enum/
|       ExecutionStatus.java        - PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELED
|       Role.java                   - ADMIN, MANAGER, CEO, EMPLOYEE
|       StepType.java               - APPROVAL, NOTIFICATION, TASK
|
+-- dto/
|     Request/
|       WorkflowRequest.java
|       StepRequest.java
|       RuleRequest.java
|       ExecutionRequest.java
|       RegisterRequest.java
|     Response/
|       WorkflowResponse.java
|       StepResponse.java
|       RuleResponse.java
|       ExecutionResponse.java
|       UserResponse.java
|
+-- repo/
|     WorkflowRepo.java
|     StepRepo.java
|     RuleRepo.java
|     ExecutionRepo.java
|     UserRepo.java
```

---

## Database Schema

```sql
users
  id              VARCHAR(36)   PRIMARY KEY
  name            VARCHAR(255)
  email           VARCHAR(255)  UNIQUE
  password        VARCHAR(255)
  role            ENUM('ADMIN','MANAGER','CEO','EMPLOYEE')

workflows
  id              BINARY(16)    PRIMARY KEY
  name            VARCHAR(255)
  version         INT
  is_active       TINYINT(1)
  input_schema    TEXT
  start_step_id   BINARY(16)
  created_at      DATETIME
  updated_at      DATETIME

steps
  id              BINARY(16)    PRIMARY KEY
  workflow_id     BINARY(16)
  name            VARCHAR(255)
  step_type       ENUM('APPROVAL','NOTIFICATION','TASK')
  step_order      INT
  metadata        TEXT
  created_at      DATETIME
  updated_at      DATETIME

rules
  id              BINARY(16)    PRIMARY KEY
  step_id         BINARY(16)
  condition_expr  TEXT
  next_step_id    BINARY(16)    NULL  (NULL means workflow ends)
  priority        INT
  created_at      DATETIME
  updated_at      DATETIME

executions
  id              BINARY(16)    PRIMARY KEY
  workflow_id     BINARY(16)
  workflow_version INT
  status          ENUM('PENDING','IN_PROGRESS','COMPLETED','FAILED','CANCELED')
  input_data      TEXT          (JSON)
  logs            TEXT          (JSON array of step log entries)
  current_step_id BINARY(16)
  retries         INT
  triggered_by    VARCHAR(36)
  started_at      DATETIME
  ended_at        DATETIME
```

---

## API Reference

### Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | /api/auth/register | Public | Register as EMPLOYEE |
| POST | /api/auth/login | Public | Login and create session |
| GET | /api/auth/me | Authenticated | Get current user |
| POST | /api/auth/logout | Authenticated | Destroy session |
| GET | /api/auth/hash | Public | Generate BCrypt hash (dev only) |

### Workflows

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | /api/workflows | Authenticated | List all active workflows |
| GET | /api/workflows/{id} | Authenticated | Get single workflow |
| GET | /api/workflows/search?name= | Authenticated | Search by name |
| POST | /api/workflows | Authenticated | Create workflow |
| PUT | /api/workflows/{id} | ADMIN | Update workflow (creates new version) |
| DELETE | /api/workflows/{id} | ADMIN | Soft delete workflow |
| PUT | /api/workflows/{id}/start-step/{stepId} | ADMIN | Set start step |

### Steps

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | /api/workflows/{id}/steps | Authenticated | List steps for workflow |
| POST | /api/workflows/{id}/steps | ADMIN | Add step |
| PUT | /api/steps/{id} | ADMIN | Update step |
| DELETE | /api/steps/{id} | ADMIN | Delete step |

### Rules

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | /api/steps/{id}/rules | Authenticated | List rules for step |
| POST | /api/steps/{id}/rules | ADMIN | Add rule |
| PUT | /api/rules/{id} | ADMIN | Update rule |
| DELETE | /api/rules/{id} | ADMIN | Delete rule |

### Executions

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | /api/workflows/{id}/execute | Authenticated | Start execution |
| GET | /api/executions | Authenticated | List all executions |
| GET | /api/executions/{id} | Authenticated | Get single execution |
| GET | /api/executions/pending?email= | MANAGER, CEO, ADMIN | Get pending approvals by assignee email |
| POST | /api/executions/{id}/approve | Authenticated | Approve current step |
| POST | /api/executions/{id}/reject | Authenticated | Reject with comment |
| POST | /api/executions/{id}/cancel | Authenticated | Cancel execution |
| POST | /api/executions/{id}/retry | Authenticated | Retry failed execution |

### Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | /api/users | Authenticated | List all users |
| POST | /api/users | ADMIN | Create user with specific role |
| PUT | /api/users/{id}/role | ADMIN | Update user role |
| DELETE | /api/users/{id} | ADMIN | Delete user |

---

## Rule Engine

Rules are evaluated using the MVEL expression language. Each step can have multiple rules ordered by priority. The engine evaluates rules in ascending priority order and returns the first match.

### Supported Syntax

| Operator | Example |
|---|---|
| Equals | `priority == 'High'` |
| Not equals | `status != 'pending'` |
| Greater than | `amount > 5000` |
| Less than | `days < 10` |
| Logical AND | `amount > 5000 && priority == 'High'` |
| Logical OR | `department == 'IT' \|\| department == 'Finance'` |
| Catch-all | `DEFAULT` |

### Important Notes

- Use `&&` and `||` not `AND` and `OR`
- Use `==` not `=` for equality checks
- Always add a `DEFAULT` rule as the last rule on every step
- If a rule has no next step (null), the workflow completes at that step
- Steps named with "reject" in the name will mark the execution as FAILED when reached

### Example Rule Set for Manager Approval Step

```
Priority 1: amount > 5000 && priority == 'High'    --> CEO Approval
Priority 2: amount <= 5000 && priority == 'Low'    --> Finance Notification
Priority 3: amount <= 5000 && priority == 'Medium' --> Finance Notification
Priority 4: DEFAULT                                 --> Task Rejection
```

---

## Email Notification

When a NOTIFICATION step is reached during execution, the engine automatically sends an email to the configured recipient using Spring Boot Mail. This is used to notify the finance team when an expense request passes the manager approval stage.

The notification step metadata specifies the channel:

```json
{"channel": "email"}
```

### Configuration

Add the following to `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

For Gmail, generate an App Password from your Google Account security settings and use it as `spring.mail.password`.

---

## Installation Guide

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- MySQL 8.0 or higher
- Git

### Step 1 - Clone the repository

```bash
git clone https://github.com/Hallyex-Workflow-Engine/workflow-engine-backend.git
cd workflow-engine-backend
```

### Step 2 - Create the database

```sql
CREATE DATABASE workflow_engine;
```

### Step 3 - Configure application.properties

Open `src/main/resources/application.properties` and set:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/workflow_engine
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

server.port=8080

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

server.servlet.session.cookie.same-site=none
server.servlet.session.cookie.secure=false
server.servlet.session.cookie.http-only=true
```

### Step 4 - Build and run

```bash
mvn clean install
mvn spring-boot:run
```

The server will start at `http://localhost:8080`.

### Step 5 - Seed initial users

Generate BCrypt hashes for your passwords using the dev endpoint:

```
GET http://localhost:8080/api/auth/hash?password=admin123
GET http://localhost:8080/api/auth/hash?password=manager123
GET http://localhost:8080/api/auth/hash?password=ceo123
GET http://localhost:8080/api/auth/hash?password=emp123
```

Then insert users into the database:

```sql
ALTER TABLE users
MODIFY COLUMN role ENUM('ADMIN','EMPLOYEE','MANAGER','CEO') NOT NULL;

INSERT INTO users (id, name, email, password, role) VALUES
  (UUID(), 'Admin User',    'admin@company.com',   '<admin_hash>',   'ADMIN'),
  (UUID(), 'Mani Manager',  'manager@company.com', '<manager_hash>', 'MANAGER'),
  (UUID(), 'Arjun CEO',     'ceo@company.com',     '<ceo_hash>',     'CEO'),
  (UUID(), 'Ravi Employee', 'emp@company.com',     '<emp_hash>',     'EMPLOYEE');
```

---

## Configuration

### CORS

CORS is configured in `SecurityConfig.java` to allow the frontend origin:

```java
configuration.setAllowedOrigins(List.of("http://localhost:5174"));
configuration.setAllowCredentials(true);
configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
```

Update the allowed origin when deploying to production.

### Session Management

The application uses HTTP session-based authentication. Sessions are stored server-side. The `HttpSessionSecurityContextRepository` is used to persist the security context across requests.

---

## Testing

### Verify the server is running

```
GET http://localhost:8080/api/auth/hash?password=test
```

Expected response: a BCrypt hash string.

### Test login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@company.com","password":"admin123"}' \
  -c cookies.txt
```

### Test authenticated request

```bash
curl http://localhost:8080/api/workflows \
  -b cookies.txt
```

---

## Workflow Versioning

When an ADMIN updates a workflow, the system does not overwrite the existing record. Instead it:

1. Marks the current workflow version as inactive
2. Creates a new workflow record with an incremented version number
3. Copies all steps to the new workflow with new UUIDs
4. Re-maps all rule next_step_id references to the new step UUIDs
5. Carries over the start step assignment using the ID mapping

This ensures that in-progress executions referencing the old version are not disrupted.

---

## Organization

Hallyex-Workflow-Engine repositories: https://github.com/Hallyex-Workflow-Engine

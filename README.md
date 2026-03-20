#  Workflow Engine (Spring Boot + React)

A dynamic Workflow Engine that allows users to create, execute, and manage workflows with approval steps and rule-based decision making.

---

##  Tech Stack

### Backend (Spring Boot)
- Java 17+
- Spring Boot
- Spring Security (Session-based Auth)
- Spring Data JPA
- MySQL
- ModelMapper
- MVEL (Rule Engine)

### Frontend (React)
- React (Vite)
- Axios
- Context API (Auth)
- Basic CSS

---

##  Project Setup

### Backend (Spring Initializer)

Create project using:
- Project: Maven
- Language: Java
- Spring Boot: 3.x
- Dependencies:
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - MySQL Driver
  - Lombok

Run backend:
mvn spring-boot:run

---

###  Frontend (React)

Create React app using Vite:

npm create vite@latest
cd project-name
npm install
npm run dev

Install dependencies:

npm install axios react-router-dom

---

##  Authentication

- Session-based authentication using JSESSIONID
- Role-based access:
  - ADMIN
  - USER (Manager / CEO approvals)

---

##  Workflow Overview

A workflow consists of:
- Multiple Steps
- Each step can be:
  - APPROVAL (manual)
  - SYSTEM (automatic)

Execution flow:
1. User starts workflow
2. Moves step-by-step
3. Approval steps wait for user action
4. Rules decide next step

---

##  Rule Engine (MVEL)

The system uses MVEL (Expression Language) to evaluate rules dynamically.

### Example Rules:
amount > 100 && priority == 'High' -> Finance Approval  
amount <= 100 -> CEO Approval  
true -> Task Rejection  

Rules are stored in DB and evaluated at runtime.

---

##  Workflow Diagram

Start  
 ↓  
Manager Approval  
 ↓  
CEO Approval  
 ↓  
Finance / Reject  
 ↓  
End  

---

##  Features

- Create and manage workflows
- Dynamic step execution
- Rule-based routing
- Approval system (Manager / CEO)
- Execution tracking with logs
- Dashboard for pending approvals

---

##  Future Enhancements

- Drag-and-drop workflow builder
- Rule builder UI
- Email notifications
- AI-based recommendations

---

##  Author

Nandha Kishor
## Demo video Link
https://youtu.be/1mdkGGW-WjU

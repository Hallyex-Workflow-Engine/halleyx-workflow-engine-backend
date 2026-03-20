package com.halleyx.workflow_engine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync   // enables @Async on EmailService so emails don't block the thread
public class AsyncConfig {
}
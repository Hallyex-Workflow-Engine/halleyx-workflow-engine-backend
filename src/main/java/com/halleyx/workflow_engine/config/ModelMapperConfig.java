package com.halleyx.workflow_engine.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// model mapperr for convert dto -> entity and entity -> dto
public class ModelMapperConfig {

    @Bean
    public ModelMapper ModelMapper(){
        return new ModelMapper();
    }
}

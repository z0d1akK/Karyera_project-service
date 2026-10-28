package com.zodiakk.projectservice;

import com.zodiakk.projectservice.outbox.config.OutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class ProjectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectServiceApplication.class, args);
    }

}
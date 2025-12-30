package com.testexecutionplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TestExecutionPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestExecutionPlatformApplication.class, args);
    }

}
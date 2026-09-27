package com.example.learningagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LearningAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(LearningAgentApplication.class, args);
    }
}

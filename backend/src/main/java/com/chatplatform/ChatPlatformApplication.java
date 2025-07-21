package com.chatplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableJpaRepositories(basePackages = "com.chatplatform.repository.jpa")
@EnableMongoRepositories(basePackages = "com.chatplatform.repository.mongo")
@EnableAsync
@EnableScheduling
public class ChatPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatPlatformApplication.class, args);
    }
}
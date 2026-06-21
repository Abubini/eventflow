package com.ctbe.eventflow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication @EnableScheduling
public class EventflowApplication {
    public static void main(String[] args) { SpringApplication.run(EventflowApplication.class, args); }
}

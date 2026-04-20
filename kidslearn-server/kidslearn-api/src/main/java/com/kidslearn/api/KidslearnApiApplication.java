package com.kidslearn.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.kidslearn.common", "com.kidslearn.api"})
public class KidslearnApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KidslearnApiApplication.class, args);
    }
}

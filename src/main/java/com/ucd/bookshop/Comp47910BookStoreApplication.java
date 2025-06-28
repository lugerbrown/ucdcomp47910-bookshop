package com.ucd.bookshop;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Comp47910BookStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(Comp47910BookStoreApplication.class, args);
    }
    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
                System.out.println("Spring Boot bookshop application started successfully! 📖");
        }; }
}

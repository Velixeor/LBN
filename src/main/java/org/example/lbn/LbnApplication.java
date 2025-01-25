package org.example.lbn;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class LbnApplication {

    public static void main(String[] args) {
        SpringApplication.run(LbnApplication.class, args);
    }

}

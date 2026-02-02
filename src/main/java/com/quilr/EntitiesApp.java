package com.quilr;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

/**
 * Main application class for quilr-entities-service
 * 
 * @author visheshchaitanya
 */
@SpringBootApplication
@Log4j2
@ComponentScan(basePackages = { "com.quilr"})
@EntityScan("com.quilr.entities")
public class EntitiesApp {

    public static void main(String[] args) {
        SpringApplication.run(EntitiesApp.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

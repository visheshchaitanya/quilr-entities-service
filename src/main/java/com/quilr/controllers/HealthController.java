package com.quilr.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/entities")
@Log4j2
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Health check endpoint called");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "quilr-entities-service"
        ));
    }
    
    @GetMapping("/kafka/info")
    public ResponseEntity<Map<String, Object>> kafkaInfo() {
        log.debug("Kafka info endpoint called");
        return ResponseEntity.ok(Map.of(
            "message", "Kafka consumer running",
            "service", "quilr-entities-service"
        ));
    }
}

package com.quilr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quilr.dto.TransformedEntity;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of EntityOutputService.
 * Handles transformed entity output based on configured mode.
 * 
 * Output modes:
 * - LOG: Log transformed entities in JSON format (default)
 * - DATABASE: Persist to PostgreSQL via EntityIngestionService
 * - KAFKA: Forward to output Kafka topic (future)
 * - REST_API: Send to REST API endpoints (future)
 */
@Service
@Log4j2
public class EntityOutputServiceImpl implements EntityOutputService {
    
    private final ObjectMapper objectMapper;
    private final EntityIngestionService ingestionService;
    
    @Value("${quilr.transformers.output.mode:LOG}")
    private String outputMode;
    
    public EntityOutputServiceImpl(
            ObjectMapper objectMapper,
            EntityIngestionService ingestionService) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
    }
    
    @Override
    public void handleTransformedEntity(TransformedEntity entity) {
        if (entity == null) {
            log.warn("Received null TransformedEntity, skipping output");
            return;
        }
        
        try {
            // Log transformed entity (for debugging)
            if (log.isDebugEnabled()) {
                String jsonOutput = objectMapper.writeValueAsString(entity);
                log.debug("TRANSFORMED_ENTITY: {}", jsonOutput);
            }
            
            // Handle based on output mode
            switch (outputMode.toUpperCase()) {
                case "DATABASE":
                    handleDatabaseOutput(entity);
                    break;
                case "KAFKA":
                    handleKafkaOutput(entity);
                    break;
                case "REST_API":
                    handleRestApiOutput(entity);
                    break;
                case "LOG":
                default:
                    handleLogOutput(entity);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error handling transformed entity - EntityId: {}, Error: {}", 
                entity.getEntityId(), e.getMessage(), e);
            throw new RuntimeException("Failed to handle transformed entity", e);
        }
    }
    
    /**
     * Handle LOG output mode - log entity as JSON
     */
    private void handleLogOutput(TransformedEntity entity) {
        try {
            String jsonOutput = objectMapper.writeValueAsString(entity);
            log.info("TRANSFORMED_ENTITY: {}", jsonOutput);
        } catch (Exception e) {
            log.error("Error logging transformed entity: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handle DATABASE output mode - persist to PostgreSQL
     */
    private void handleDatabaseOutput(TransformedEntity entity) {
        try {
            ingestionService.ingestTransformedEntity(entity);
            log.info("Successfully ingested entity to database - Vendor: {}, Type: {}, EntityId: {}", 
                entity.getVendor(), entity.getEntityType(), entity.getEntityId());
        } catch (Exception e) {
            log.error("Error ingesting entity to database - EntityId: {}, Error: {}", 
                entity.getEntityId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Handle KAFKA output mode - forward to output topic
     * TODO: Implement Kafka producer
     */
    private void handleKafkaOutput(TransformedEntity entity) {
        log.warn("KAFKA output mode not yet implemented - EntityId: {}", entity.getEntityId());
        // TODO: Implement Kafka producer to forward transformed entity
    }
    
    /**
     * Handle REST_API output mode - send to REST API
     * TODO: Implement REST API client
     */
    private void handleRestApiOutput(TransformedEntity entity) {
        log.warn("REST_API output mode not yet implemented - EntityId: {}", entity.getEntityId());
        // TODO: Implement REST API client to forward transformed entity
    }
}


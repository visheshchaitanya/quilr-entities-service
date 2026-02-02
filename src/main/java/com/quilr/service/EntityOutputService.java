package com.quilr.service;

import com.quilr.dto.TransformedEntity;

/**
 * Service interface for handling transformed entity output.
 * Provides abstraction for different output modes: logging, Kafka, database, REST API.
 * 
 * Initial implementation will be logging only.
 * Future implementations can persist to database, forward to Kafka, or call REST APIs.
 */
public interface EntityOutputService {
    
    /**
     * Handle the transformed entity output.
     * Implementation depends on configured output mode.
     * 
     * @param entity Transformed entity to output
     */
    void handleTransformedEntity(TransformedEntity entity);
}

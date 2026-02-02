package com.quilr.service;

import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;

/**
 * Service interface for processing entity messages.
 * Orchestrates the transformation pipeline: parse → route → transform.
 */
public interface EntityProcessingService {
    
    /**
     * Process a raw entity message through the transformation pipeline.
     * 
     * Flow:
     * 1. Validate message
     * 2. Get vendor from message
     * 3. Get appropriate transformer from factory
     * 4. Transform message
     * 5. Return transformed entity
     * 
     * @param message Raw entity message from Kafka
     * @return Transformed entity in standardized format
     * @throws IllegalArgumentException if message is invalid or vendor not supported
     */
    TransformedEntity processEntity(RawEntityMessage message);
}

package com.quilr.service;

import com.quilr.dto.TransformedEntity;

/**
 * Service interface for ingesting transformed entities into PostgreSQL.
 * Handles upsert operations with parent validation and transaction management.
 */
public interface EntityIngestionService {
    
    /**
     * Ingest transformed entity into database.
     * Performs upserts for all entities and junction tables.
     * 
     * @param entity Transformed entity to ingest
     * @throws IllegalArgumentException if entity validation fails
     * @throws RuntimeException if database operation fails
     */
    void ingestTransformedEntity(TransformedEntity entity);
}

package com.quilr.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * Base repository class with common helper methods.
 */
@Log4j2
public abstract class BaseRepository {
    
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * Convert Map to JSON string, handling exceptions gracefully.
     * Returns null if map is null or conversion fails.
     */
    protected String toJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize map to JSON: {}", e.getMessage());
            return null;
        }
    }
}

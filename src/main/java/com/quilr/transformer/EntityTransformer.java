package com.quilr.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;

/**
 * Second-level transformer interface for entity-type specific transformations.
 * Each vendor's FabricTransformer delegates to this based on entity type (users/apps).
 * 
 * This is the Strategy pattern implementation:
 * - transformUsers() handles user entities
 * - transformApps() handles application entities
 * 
 * Each vendor implements this interface with vendor-specific logic.
 */
public interface EntityTransformer {
    
    /**
     * Transform user entity from vendor-specific format to standardized format.
     * 
     * @param payload Vendor-specific user data as JsonNode
     * @param context Original message context (vendor, timestamp, etc.)
     * @return Transformed user entity
     * @throws UnsupportedOperationException if users transformation not implemented
     */
    TransformedEntity transformUsers(JsonNode payload, RawEntityMessage context);
    
    /**
     * Transform application entity from vendor-specific format to standardized format.
     * 
     * @param payload Vendor-specific app data as JsonNode
     * @param context Original message context (vendor, timestamp, etc.)
     * @return Transformed app entity
     * @throws UnsupportedOperationException if apps transformation not implemented
     */
    TransformedEntity transformApps(JsonNode payload, RawEntityMessage context);
}

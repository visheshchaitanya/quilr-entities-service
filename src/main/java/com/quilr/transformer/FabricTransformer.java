package com.quilr.transformer;

import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;
import com.quilr.dto.VendorType;

/**
 * Main transformer interface for vendor-specific transformations.
 * Each vendor (Okta, Microsoft, Ollama, PingIDP) will have its own implementation.
 * 
 * This is the first level in the transformation hierarchy:
 * FabricTransformer → EntityTransformer (users/apps)
 */
public interface FabricTransformer {
    
    /**
     * Transform a raw entity message to standardized format.
     * Delegates to appropriate EntityTransformer based on message type.
     * 
     * @param message Raw entity message from Kafka
     * @return Transformed entity in standardized format
     * @throws IllegalArgumentException if message is invalid
     * @throws UnsupportedOperationException if entity type not supported
     */
    TransformedEntity transform(RawEntityMessage message);
    
    /**
     * Check if this transformer supports the given vendor.
     * Used by FabricTransformerFactory for routing.
     * 
     * @param vendor Vendor type to check
     * @return true if this transformer handles the vendor
     */
    boolean supports(VendorType vendor);
    
    /**
     * Get the vendor type this transformer handles.
     * 
     * @return VendorType enum
     */
    VendorType getVendorType();
}

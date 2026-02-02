package com.quilr.transformer.factory;

import com.quilr.dto.VendorType;
import com.quilr.transformer.FabricTransformer;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating/retrieving vendor-specific FabricTransformers.
 * Uses Spring's auto-injection to discover all FabricTransformer beans
 * and builds a mapping of VendorType → FabricTransformer.
 * 
 * This implements the Factory pattern for dynamic transformer selection.
 */
@Component
@Log4j2
public class FabricTransformerFactory {
    
    private final Map<VendorType, FabricTransformer> transformers;
    
    /**
     * Constructor that auto-injects all FabricTransformer beans.
     * Spring will find all beans implementing FabricTransformer interface.
     * 
     * @param transformerList List of all FabricTransformer beans
     */
    public FabricTransformerFactory(List<FabricTransformer> transformerList) {
        this.transformers = new HashMap<>();
        
        if (transformerList == null || transformerList.isEmpty()) {
            log.warn("No FabricTransformer beans found. Factory initialized with empty map.");
            return;
        }
        
        // Build vendor → transformer mapping
        for (FabricTransformer transformer : transformerList) {
            VendorType vendorType = transformer.getVendorType();
            
            if (transformers.containsKey(vendorType)) {
                log.warn("Duplicate transformer found for vendor: {}. Overwriting with: {}", 
                    vendorType, transformer.getClass().getSimpleName());
            }
            
            transformers.put(vendorType, transformer);
            log.info("Registered transformer for vendor: {} -> {}", 
                vendorType, transformer.getClass().getSimpleName());
        }
        
        log.info("FabricTransformerFactory initialized with {} transformers", transformers.size());
    }
    
    /**
     * Get the appropriate transformer for the given vendor.
     * 
     * @param vendor VendorType enum
     * @return FabricTransformer for the vendor
     * @throws IllegalArgumentException if no transformer found for vendor
     */
    public FabricTransformer getTransformer(VendorType vendor) {
        if (vendor == null) {
            throw new IllegalArgumentException("VendorType cannot be null");
        }
        
        FabricTransformer transformer = transformers.get(vendor);
        
        if (transformer == null) {
            throw new IllegalArgumentException(
                String.format("No transformer registered for vendor: %s. Available vendors: %s", 
                    vendor, transformers.keySet())
            );
        }
        
        return transformer;
    }
    
    /**
     * Check if a transformer exists for the given vendor.
     * 
     * @param vendor VendorType enum
     * @return true if transformer exists
     */
    public boolean hasTransformer(VendorType vendor) {
        return vendor != null && transformers.containsKey(vendor);
    }
    
    /**
     * Get all registered vendor types.
     * 
     * @return Set of VendorType enums
     */
    public java.util.Set<VendorType> getRegisteredVendors() {
        return transformers.keySet();
    }
}

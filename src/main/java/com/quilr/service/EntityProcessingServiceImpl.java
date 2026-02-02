package com.quilr.service;

import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;
import com.quilr.dto.VendorType;
import com.quilr.transformer.FabricTransformer;
import com.quilr.transformer.factory.FabricTransformerFactory;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * Implementation of EntityProcessingService.
 * Orchestrates the transformation pipeline using FabricTransformerFactory.
 */
@Service
@Log4j2
public class EntityProcessingServiceImpl implements EntityProcessingService {
    
    private final FabricTransformerFactory transformerFactory;
    
    public EntityProcessingServiceImpl(FabricTransformerFactory transformerFactory) {
        this.transformerFactory = transformerFactory;
    }
    
    @Override
    public TransformedEntity processEntity(RawEntityMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("RawEntityMessage cannot be null");
        }
        
        log.debug("Processing entity - Vendor: {}, Type: {}", message.getVendor(), message.getType());
        
        try {
            // Get vendor type from message
            VendorType vendorType = message.getVendorType();
            
            // Get appropriate transformer from factory
            FabricTransformer transformer = transformerFactory.getTransformer(vendorType);
            
            // Transform the message
            TransformedEntity result = transformer.transform(message);
            
            log.info("Successfully processed entity - Vendor: {}, Type: {}, EntityId: {}", 
                message.getVendor(), message.getType(), result.getEntityId());
            
            return result;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid message or unsupported vendor - Vendor: {}, Type: {}, Error: {}", 
                message.getVendor(), message.getType(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error processing entity - Vendor: {}, Type: {}, Error: {}", 
                message.getVendor(), message.getType(), e.getMessage(), e);
            throw new RuntimeException("Failed to process entity", e);
        }
    }
}

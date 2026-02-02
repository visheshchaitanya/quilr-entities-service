package com.quilr.transformer;

import com.quilr.dto.EntityType;
import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;
import com.quilr.dto.VendorType;
import lombok.extern.log4j.Log4j2;

/**
 * Abstract base implementation of FabricTransformer.
 * Provides common transformation logic and delegates to vendor-specific EntityTransformer
 * based on the entity type (users/apps).
 * 
 * Subclasses must:
 * 1. Inject their vendor-specific EntityTransformer
 * 2. Specify their VendorType
 * 3. Be annotated with @Component for Spring auto-detection
 */
@Log4j2
public abstract class AbstractFabricTransformer implements FabricTransformer {
    
    private final VendorType vendorType;
    private final EntityTransformer entityTransformer;
    
    /**
     * Constructor for subclasses
     * @param vendorType The vendor this transformer handles
     * @param entityTransformer Vendor-specific entity transformer
     */
    protected AbstractFabricTransformer(VendorType vendorType, EntityTransformer entityTransformer) {
        this.vendorType = vendorType;
        this.entityTransformer = entityTransformer;
    }
    
    @Override
    public TransformedEntity transform(RawEntityMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("RawEntityMessage cannot be null");
        }
        
        log.info("Transforming message - Vendor: {}, Type: {}", message.getVendor(), message.getType());
        
        try {
            // Validate vendor matches
            VendorType messageVendor = message.getVendorType();
            if (!supports(messageVendor)) {
                throw new IllegalArgumentException(
                    String.format("Transformer for vendor %s cannot handle vendor %s", 
                        vendorType, messageVendor)
                );
            }
            
            // Get entity type and delegate to appropriate transformer
            EntityType entityType = message.getEntityType();
            TransformedEntity result;
            
            switch (entityType) {
                case USERS:
                    log.debug("Delegating to transformUsers for vendor: {}", vendorType);
                    result = entityTransformer.transformUsers(message.getData(), message);
                    break;
                    
                case APPS:
                    log.debug("Delegating to transformApps for vendor: {}", vendorType);
                    result = entityTransformer.transformApps(message.getData(), message);
                    break;
                    
                default:
                    throw new UnsupportedOperationException(
                        String.format("Entity type %s not supported for vendor %s", 
                            entityType, vendorType)
                    );
            }
            
            log.info("Successfully transformed message - Vendor: {}, Type: {}, EntityId: {}", 
                message.getVendor(), message.getType(), result.getEntityId());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error transforming message - Vendor: {}, Type: {}, Error: {}", 
                message.getVendor(), message.getType(), e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public boolean supports(VendorType vendor) {
        return this.vendorType == vendor;
    }
    
    @Override
    public VendorType getVendorType() {
        return this.vendorType;
    }
    
    /**
     * Get the entity transformer instance
     * @return EntityTransformer
     */
    protected EntityTransformer getEntityTransformer() {
        return entityTransformer;
    }
}

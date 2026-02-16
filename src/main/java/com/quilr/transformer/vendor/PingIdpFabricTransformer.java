package com.quilr.transformer.vendor;

import com.quilr.dto.VendorType;
import com.quilr.transformer.AbstractFabricTransformer;
import com.quilr.transformer.DynamicEntityTransformer;
import org.springframework.stereotype.Component;

/**
 * PingIDP vendor FabricTransformer.
 * Delegates to DynamicEntityTransformer for database-driven entity transformations.
 * 
 * This is the first-level transformer in the hierarchy:
 * PingIdpFabricTransformer → DynamicEntityTransformer → transformUsers/transformApps
 */
@Component
public class PingIdpFabricTransformer extends AbstractFabricTransformer {
    
    /**
     * Constructor that injects dynamic entity transformer.
     * Spring will auto-wire the DynamicEntityTransformer bean.
     * 
     * @param entityTransformer Dynamic entity transformer
     */
    public PingIdpFabricTransformer(DynamicEntityTransformer entityTransformer) {
        super(VendorType.PING_IDP, entityTransformer);
    }
}

package com.quilr.transformer.vendor;

import com.quilr.dto.VendorType;
import com.quilr.transformer.AbstractFabricTransformer;
import com.quilr.transformer.DynamicEntityTransformer;
import org.springframework.stereotype.Component;

/**
 * Microsoft vendor FabricTransformer.
 * Delegates to DynamicEntityTransformer for database-driven entity transformations.
 * 
 * This is the first-level transformer in the hierarchy:
 * MicrosoftFabricTransformer → DynamicEntityTransformer → transformUsers/transformApps
 */
@Component
public class MicrosoftFabricTransformer extends AbstractFabricTransformer {
    
    /**
     * Constructor that injects dynamic entity transformer.
     * Spring will auto-wire the DynamicEntityTransformer bean.
     * 
     * @param entityTransformer Dynamic entity transformer
     */
    public MicrosoftFabricTransformer(DynamicEntityTransformer entityTransformer) {
        super(VendorType.MICROSOFT, entityTransformer);
    }
}

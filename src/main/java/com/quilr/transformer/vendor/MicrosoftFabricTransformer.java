package com.quilr.transformer.vendor;

import com.quilr.dto.VendorType;
import com.quilr.transformer.AbstractFabricTransformer;
import com.quilr.transformer.vendor.microsoft.MicrosoftEntityTransformer;
import org.springframework.stereotype.Component;

/**
 * Microsoft vendor FabricTransformer.
 * Delegates to MicrosoftEntityTransformer for entity-specific transformations.
 * 
 * This is the first-level transformer in the hierarchy:
 * MicrosoftFabricTransformer → MicrosoftEntityTransformer → transformUsers/transformApps
 */
@Component
public class MicrosoftFabricTransformer extends AbstractFabricTransformer {
    
    /**
     * Constructor that injects Microsoft-specific entity transformer.
     * Spring will auto-wire the MicrosoftEntityTransformer bean.
     * 
     * @param entityTransformer Microsoft entity transformer
     */
    public MicrosoftFabricTransformer(MicrosoftEntityTransformer entityTransformer) {
        super(VendorType.MICROSOFT, entityTransformer);
    }
}

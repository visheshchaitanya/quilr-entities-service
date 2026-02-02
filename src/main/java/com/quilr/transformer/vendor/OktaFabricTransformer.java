package com.quilr.transformer.vendor;

import com.quilr.dto.VendorType;
import com.quilr.transformer.AbstractFabricTransformer;
import com.quilr.transformer.vendor.okta.OktaEntityTransformer;
import org.springframework.stereotype.Component;

/**
 * Okta vendor FabricTransformer.
 * STUB IMPLEMENTATION - Not yet implemented.
 * 
 * This is registered in the factory to demonstrate the architecture,
 * but all transformation methods throw NotImplementedException.
 */
@Component
public class OktaFabricTransformer extends AbstractFabricTransformer {
    
    public OktaFabricTransformer(OktaEntityTransformer entityTransformer) {
        super(VendorType.OKTA, entityTransformer);
    }
}

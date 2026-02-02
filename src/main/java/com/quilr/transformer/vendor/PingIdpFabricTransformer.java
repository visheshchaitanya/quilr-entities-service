package com.quilr.transformer.vendor;

import com.quilr.dto.VendorType;
import com.quilr.transformer.AbstractFabricTransformer;
import com.quilr.transformer.vendor.pingidp.PingIdpEntityTransformer;
import org.springframework.stereotype.Component;

/**
 * PingIDP vendor FabricTransformer.
 * STUB IMPLEMENTATION - Not yet implemented.
 * 
 * This is registered in the factory to demonstrate the architecture,
 * but all transformation methods throw NotImplementedException.
 */
@Component
public class PingIdpFabricTransformer extends AbstractFabricTransformer {
    
    public PingIdpFabricTransformer(PingIdpEntityTransformer entityTransformer) {
        super(VendorType.PING_IDP, entityTransformer);
    }
}

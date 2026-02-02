package com.quilr.transformer.vendor;

import com.quilr.dto.VendorType;
import com.quilr.transformer.AbstractFabricTransformer;
import com.quilr.transformer.vendor.ollama.OllamaEntityTransformer;
import org.springframework.stereotype.Component;

/**
 * Ollama vendor FabricTransformer.
 * STUB IMPLEMENTATION - Not yet implemented.
 * 
 * This is registered in the factory to demonstrate the architecture,
 * but all transformation methods throw NotImplementedException.
 */
@Component
public class OllamaFabricTransformer extends AbstractFabricTransformer {
    
    public OllamaFabricTransformer(OllamaEntityTransformer entityTransformer) {
        super(VendorType.OLLAMA, entityTransformer);
    }
}

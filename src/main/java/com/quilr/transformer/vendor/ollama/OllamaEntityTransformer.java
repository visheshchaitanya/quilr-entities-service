package com.quilr.transformer.vendor.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;
import com.quilr.transformer.EntityTransformer;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

/**
 * Ollama-specific entity transformer.
 * STUB IMPLEMENTATION - Not yet implemented.
 * 
 * This is a placeholder to demonstrate the architecture.
 * Detailed implementation will be added later.
 */
@Component
public class OllamaEntityTransformer implements EntityTransformer {
    
    @Override
    public TransformedEntity transformUsers(JsonNode payload, RawEntityMessage context) {
        throw new NotImplementedException(
            "Ollama users transformation not yet implemented. " +
            "This is a stub transformer for architecture demonstration."
        );
    }
    
    @Override
    public TransformedEntity transformApps(JsonNode payload, RawEntityMessage context) {
        throw new NotImplementedException(
            "Ollama apps transformation not yet implemented. " +
            "This is a stub transformer for architecture demonstration."
        );
    }
}

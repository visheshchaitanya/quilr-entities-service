package com.quilr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for transformer pipeline.
 * Maps to quilr.transformers.* properties in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "quilr.transformers")
@Data
public class TransformerConfig {
    
    /**
     * Master switch to enable/disable transformation pipeline
     * If false, consumer falls back to logging only
     */
    private boolean enabled = false;
    
    /**
     * Vendor-specific configuration
     * Key: vendor name (microsoft, okta, ollama, pingidp)
     * Value: VendorConfig with enabled flag
     */
    private Map<String, VendorConfig> vendors = new HashMap<>();
    
    /**
     * Output configuration
     */
    private OutputConfig output = new OutputConfig();
    
    @Data
    public static class VendorConfig {
        /**
         * Enable/disable specific vendor transformer
         */
        private boolean enabled = false;
    }
    
    @Data
    public static class OutputConfig {
        /**
         * Output mode: LOG, KAFKA, DATABASE, REST_API
         */
        private String mode = "LOG";
    }
    
    /**
     * Check if a specific vendor is enabled
     */
    public boolean isVendorEnabled(String vendorName) {
        if (!enabled) {
            return false;
        }
        VendorConfig vendorConfig = vendors.get(vendorName.toLowerCase());
        return vendorConfig != null && vendorConfig.isEnabled();
    }
}

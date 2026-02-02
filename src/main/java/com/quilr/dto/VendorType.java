package com.quilr.dto;

/**
 * Enum representing IDP vendor types.
 * Used by FabricTransformerFactory to route messages to appropriate transformer.
 */
public enum VendorType {
    OKTA("okta"),
    MICROSOFT("microsoft"),
    OLLAMA("ollama"),
    PING_IDP("pingidp");

    private final String value;

    VendorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse string value to VendorType enum
     * @param value String representation of vendor
     * @return VendorType enum
     * @throws IllegalArgumentException if value doesn't match any enum
     */
    public static VendorType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("VendorType value cannot be null");
        }
        
        for (VendorType vendor : VendorType.values()) {
            if (vendor.value.equalsIgnoreCase(value)) {
                return vendor;
            }
        }
        
        throw new IllegalArgumentException("Unknown VendorType: " + value);
    }
}

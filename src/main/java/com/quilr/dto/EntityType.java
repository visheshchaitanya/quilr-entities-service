package com.quilr.dto;

/**
 * Enum representing the type of entity being processed.
 * Used to route messages to appropriate transformer strategy.
 */
public enum EntityType {
    USERS("users"),
    APPS("apps");

    private final String value;

    EntityType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse string value to EntityType enum
     * @param value String representation of entity type
     * @return EntityType enum
     * @throws IllegalArgumentException if value doesn't match any enum
     */
    public static EntityType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("EntityType value cannot be null");
        }
        
        for (EntityType type : EntityType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unknown EntityType: " + value);
    }
}

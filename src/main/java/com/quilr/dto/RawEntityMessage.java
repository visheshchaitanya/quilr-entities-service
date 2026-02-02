package com.quilr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing the raw incoming message from Kafka.
 * Matches the structure of sample_ms_users_payload.json with metadata fields
 * at root level and actual entity data in 'data' field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawEntityMessage {
    
    /**
     * Relation type (e.g., "HAS_IDP")
     */
    @JsonProperty("relation_type")
    private String relationType;
    
    /**
     * Sub-product path (e.g., "/v1.0/users")
     */
    @JsonProperty("subProduct")
    private String subProduct;
    
    /**
     * Domain (e.g., "login.microsoftonline.com")
     */
    private String domain;
    
    /**
     * Entity type (users, apps, groups, etc.)
     */
    @NotNull(message = "Type field is required")
    private String type;
    
    /**
     * Product identifier (e.g., "microsoft.graph")
     */
    private String product;
    
    /**
     * Subscriber UUID
     */
    private String subscriber;
    
    /**
     * Connector name (e.g., "Active Directory Users Connector")
     */
    private String name;
    
    /**
     * Connector description
     */
    private String description;
    
    /**
     * Instance UUID
     */
    @JsonProperty("instance_id")
    private String instanceId;
    
    /**
     * Tenant UUID
     */
    private String tenant;
    
    /**
     * IDP vendor identifier (Microsoft, Okta, etc.)
     */
    @NotNull(message = "Vendor field is required")
    private String vendor;
    
    /**
     * Actual entity data - contains user/app/group information
     * This is the nested 'data' object in the payload
     */
    @NotNull(message = "Data field is required")
    private JsonNode data;
    
    /**
     * Timestamp of the message (optional, can be added by consumer)
     */
    private Instant timestamp;
    
    /**
     * Get vendor as enum
     */
    public VendorType getVendorType() {
        return VendorType.fromValue(vendor);
    }
    
    /**
     * Get entity type as enum
     */
    public EntityType getEntityType() {
        return EntityType.fromValue(type);
    }
    
    /**
     * Get subscriber as UUID
     */
    public UUID getSubscriberUuid() {
        return subscriber != null ? UUID.fromString(subscriber) : null;
    }
    
    /**
     * Get tenant as UUID
     */
    public UUID getTenantUuid() {
        return tenant != null ? UUID.fromString(tenant) : null;
    }
    
    /**
     * Get instance as UUID
     */
    public UUID getInstanceUuid() {
        return instanceId != null ? UUID.fromString(instanceId) : null;
    }
}

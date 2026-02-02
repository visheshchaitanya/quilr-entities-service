package com.quilr.dto.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity DTO representing tenant table.
 * Maps to the tenant table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantEntity {
    
    private UUID tenantId;
    
    @NotNull
    @Size(max = 255)
    private String id;
    
    @Size(max = 255)
    private String name;
    
    private UUID subscriberId;
    
    private Instant creationTime;
    
    @Builder.Default
    private Boolean enablePersonaViaBackgroundTabs = false;
    
    @Builder.Default
    private Boolean enablePersonaViaForcedLogin = false;
    
    @Builder.Default
    private Boolean extensionEnabled = false;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Map<String, Object> extraInfo;
    
    /**
     * Generate UUID if not present
     */
    public void ensureTenantId() {
        if (this.tenantId == null) {
            this.tenantId = UUID.randomUUID();
        }
    }
}

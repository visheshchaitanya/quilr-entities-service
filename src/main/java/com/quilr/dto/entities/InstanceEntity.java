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
 * Entity DTO representing instance table.
 * Maps to the instance table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceEntity {
    
    private UUID instanceId;
    
    @NotNull
    private UUID tenantId;
    
    @NotNull
    @Size(max = 255)
    private String appId;
    
    private Instant creationTime;
    
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
    public void ensureInstanceId() {
        if (this.instanceId == null) {
            this.instanceId = UUID.randomUUID();
        }
    }
}

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
 * Entity DTO representing roles table.
 * Maps to the roles table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {
    
    private UUID roleId;
    
    @NotNull
    private UUID tenantId;
    
    @NotNull
    @Size(max = 255)
    private String id;
    
    @Size(max = 255)
    private String displayName;
    
    @Size(max = 1000)
    private String description;
    
    @Builder.Default
    private Boolean isBuiltIn = false;
    
    @Builder.Default
    private Boolean isEnabled = true;
    
    @Builder.Default
    private Boolean isPrivileged = false;
    
    @Size(max = 255)
    private String roleTemplateId;
    
    @Size(max = 100)
    private String assignmentType;
    
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
    public void ensureRoleId() {
        if (this.roleId == null) {
            this.roleId = UUID.randomUUID();
        }
    }
}

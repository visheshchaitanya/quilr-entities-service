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
 * Entity DTO representing department table.
 * Maps to the department table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentEntity {
    
    private UUID departmentId;
    
    @NotNull
    private UUID tenantId;
    
    @NotNull
    @Size(max = 255)
    private String id;
    
    @Size(max = 255)
    private String name;
    
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
    public void ensureDepartmentId() {
        if (this.departmentId == null) {
            this.departmentId = UUID.randomUUID();
        }
    }
}

package com.quilr.dto.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity DTO representing user_department junction table.
 * Maps to the user_department table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDepartmentLink {
    
    @NotNull
    private UUID userId;
    
    @NotNull
    private UUID tenantId;
    
    @NotNull
    private UUID departmentId;
    
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Map<String, Object> extraInfo;
}

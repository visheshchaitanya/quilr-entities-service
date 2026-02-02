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
 * Entity DTO representing user_groups junction table.
 * Maps to the user_groups table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupLink {
    
    @NotNull
    private UUID userId;
    
    @NotNull
    private UUID groupId;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Map<String, Object> extraInfo;
}

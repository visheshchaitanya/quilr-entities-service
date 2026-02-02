package com.quilr.dto.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity DTO representing groups table.
 * Maps to the groups table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEntity {
    
    private UUID groupId;
    
    @NotNull
    private UUID tenantId;
    
    @NotNull
    @Size(max = 255)
    private String id;
    
    @Size(max = 255)
    private String displayName;
    
    @Size(max = 255)
    private String mail;
    
    @Builder.Default
    private Boolean mailEnabled = false;
    
    @Builder.Default
    private Boolean securityEnabled = false;
    
    private List<String> groupTypes;
    
    private Instant createdDateTime;
    
    @Size(max = 1000)
    private String description;
    
    @Size(max = 50)
    private String visibility;
    
    @Size(max = 100)
    private String classification;
    
    @Size(max = 255)
    private String mailNickname;
    
    @Size(max = 500)
    private String membershipRule;
    
    @Size(max = 50)
    private String membershipRuleProcessingState;
    
    @Size(max = 100)
    private String preferredDataLocation;
    
    @Size(max = 50)
    private String preferredLanguage;
    
    private Instant renewedDateTime;
    
    @Size(max = 50)
    private String theme;
    
    @Size(max = 255)
    private String uniqueName;
    
    private Boolean isAssignableToRole;
    
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
    public void ensureGroupId() {
        if (this.groupId == null) {
            this.groupId = UUID.randomUUID();
        }
    }
}

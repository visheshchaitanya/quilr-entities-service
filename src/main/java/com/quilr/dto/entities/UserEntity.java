package com.quilr.dto.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Entity DTO representing user table.
 * Maps to the user table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    
    private UUID userId;
    
    @NotNull
    private UUID tenantId;
    
    @NotNull
    private UUID instanceId;
    
    @NotNull
    @Size(max = 255)
    private String id;
    
    @Size(max = 255)
    private String displayName;
    
    @Size(max = 255)
    private String givenName;
    
    @Size(max = 255)
    private String surname;
    
    @Size(max = 255)
    private String mail;
    
    @Size(max = 255)
    private String userPrincipalName;
    
    @Size(max = 50)
    private String mobilePhone;
    
    @Size(max = 255)
    private String jobTitle;
    
    @Size(max = 100)
    private String employeeType;
    
    private LocalDate employeeHireDate;
    
    private LocalDate terminationDate;
    
    @Builder.Default
    private Boolean accountEnabled = true;
    
    @Builder.Default
    private Boolean userSuspended = false;
    
    @Builder.Default
    private Boolean userArchived = false;
    
    @Size(max = 50)
    private String userType;
    
    @Builder.Default
    private Boolean userIsAdmin = false;
    
    @Builder.Default
    private Boolean userDelegationAdmin = false;
    
    @Builder.Default
    private Boolean userIpWhitelisted = false;
    
    @Builder.Default
    private Boolean extensionEnabled = false;
    
    @Size(max = 100)
    private String extensionDeploymentStatus;
    
    private Instant userCreationTime;
    
    private Instant userLastLoginTime;
    
    @Size(max = 500)
    private String profilePicUrl;
    
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
    public void ensureUserId() {
        if (this.userId == null) {
            this.userId = UUID.randomUUID();
        }
    }
}

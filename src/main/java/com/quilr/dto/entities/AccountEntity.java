package com.quilr.dto.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity DTO representing account table.
 * Maps to the account table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {
    
    @NotNull
    @Size(max = 255)
    private String id;
    
    @NotNull
    private UUID tenantId;
    
    @Size(max = 255)
    private String email;
    
    @Size(max = 255)
    private String appName;
    
    @Size(max = 255)
    private String appId;
    
    @Size(max = 255)
    private String microsoftId;
    
    private Instant creationTime;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
}

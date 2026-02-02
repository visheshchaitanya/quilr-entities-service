package com.quilr.dto.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;

/**
 * Entity DTO representing application table.
 * Maps to the application table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEntity {
    
    @NotNull
    @Size(max = 255)
    private String id_;
    
    @Size(max = 255)
    private String domain;
    
    @Builder.Default
    private Boolean newApp = false;
    
    @Builder.Default
    private Boolean globalSyncAllowed = false;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
}

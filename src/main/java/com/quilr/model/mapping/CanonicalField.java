package com.quilr.model.mapping;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for canonical_fields table
 * Defines expected fields for each entity type
 */
@Entity
@Table(name = "canonical_fields")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalField {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;
    
    @Column(name = "data_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DataType dataType;
    
    @Column(nullable = false)
    private Boolean required = false;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

package com.quilr.model.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for field_mappings table
 * Stores mapping configuration for transforming vendor-specific fields to canonical entities
 */
@Entity
@Table(name = "field_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 50)
    private String vendor;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    
    @Column(name = "target_entity", nullable = false, length = 50)
    private String targetEntity;
    
    @Column(name = "target_field", nullable = false, length = 100)
    private String targetField;
    
    @Column(name = "source_path", nullable = false, length = 500)
    private String sourcePath;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fallback_paths", columnDefinition = "jsonb")
    private JsonNode fallbackPaths;
    
    @Column(name = "data_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DataType dataType;
    
    @Column(length = 100)
    private String transform;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transform_args", columnDefinition = "jsonb")
    private JsonNode transformArgs;
    
    @Column(length = 500)
    private String condition;
    
    @Column(name = "default_value")
    private String defaultValue;
    
    @Column(nullable = false)
    private Boolean required = false;
    
    @Column(nullable = false)
    private Integer priority = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

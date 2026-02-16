package com.quilr.model.mapping;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for transform_functions table
 * Defines available transformation functions
 */
@Entity
@Table(name = "transform_functions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformFunction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "function_type", nullable = false, length = 50)
    private String functionType;
    
    @Column(name = "implementation_class", length = 255)
    private String implementationClass;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

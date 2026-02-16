package com.quilr.repository;

import com.quilr.model.mapping.CanonicalField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for canonical_fields table
 */
@Repository
public interface CanonicalFieldRepository extends JpaRepository<CanonicalField, UUID> {
    
    /**
     * Find all canonical fields for an entity type
     */
    List<CanonicalField> findByEntityType(String entityType);
}

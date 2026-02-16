package com.quilr.repository;

import com.quilr.model.mapping.FieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for field_mappings table
 */
@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMapping, UUID> {
    
    /**
     * Find all field mappings for a vendor and entity type, ordered by priority
     */
    List<FieldMapping> findByVendorAndEntityTypeOrderByPriorityAsc(String vendor, String entityType);
    
    /**
     * Find field mappings for a specific target entity
     */
    List<FieldMapping> findByVendorAndEntityTypeAndTargetEntity(String vendor, String entityType, String targetEntity);
}

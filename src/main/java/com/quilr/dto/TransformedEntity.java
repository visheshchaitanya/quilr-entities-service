package com.quilr.dto;

import com.quilr.dto.entities.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO representing the standardized output after transformation.
 * All vendor-specific transformers produce this common format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformedEntity {
    
    /**
     * Unique identifier for the entity
     */
    private String entityId;
    
    /**
     * Type of entity (USERS, APPS)
     */
    private EntityType entityType;
    
    /**
     * Source vendor
     */
    private VendorType vendor;
    
    /**
     * Timestamp when entity was transformed
     */
    private Instant transformedAt;
    
    /**
     * Original message timestamp
     */
    private Instant originalTimestamp;
    
    /**
     * Standardized entity attributes (legacy - for backward compatibility)
     * Key-value pairs representing entity fields
     */
    private Map<String, Object> attributes;
    
    /**
     * Additional metadata about the transformation
     * Can include: transformation_version, source_system, etc.
     */
    private Map<String, Object> metadata;
    
    // ===== Structured Entity Objects =====
    
    /**
     * Tenant entity
     */
    private TenantEntity tenant;
    
    /**
     * Instance entity
     */
    private InstanceEntity instance;
    
    /**
     * Application entity
     */
    private ApplicationEntity application;
    
    /**
     * Account entity
     */
    private AccountEntity account;
    
    /**
     * User entity
     */
    private UserEntity user;
    
    /**
     * List of department entities
     */
    private List<DepartmentEntity> departments;
    
    /**
     * List of office location entities
     */
    private List<OfficeLocationEntity> officeLocations;
    
    /**
     * List of group entities
     */
    private List<GroupEntity> groups;
    
    /**
     * List of role entities
     */
    private List<RoleEntity> roles;
    
    // ===== Junction Table Links =====
    
    /**
     * User-Department links
     */
    private List<UserDepartmentLink> userDepartments;
    
    /**
     * User-Office Location links
     */
    private List<UserOfficeLocationLink> userOfficeLocations;
    
    /**
     * User-Group links
     */
    private List<UserGroupLink> userGroups;
    
    /**
     * User-Role links
     */
    private List<UserRoleLink> userRoles;
}

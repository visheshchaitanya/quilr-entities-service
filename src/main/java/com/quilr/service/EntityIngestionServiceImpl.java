package com.quilr.service;

import com.quilr.dto.TransformedEntity;
import com.quilr.dto.entities.*;
import com.quilr.repository.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of EntityIngestionService.
 * Handles transactional ingestion of transformed entities into PostgreSQL.
 * 
 * Ingestion flow:
 * 1. Validate parent references (skip if missing)
 * 2. Upsert tenant
 * 3. Upsert instance
 * 4. Upsert application (once per unique appId)
 * 5. Upsert account (for each user)
 * 6. Upsert departments, office locations, groups, roles
 * 7. Upsert user
 * 8. Upsert junction table links
 */
@Service
@Log4j2
public class EntityIngestionServiceImpl implements EntityIngestionService {
    
    private final TenantRepository tenantRepository;
    private final InstanceRepository instanceRepository;
    private final ApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficeLocationRepository officeLocationRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    
    public EntityIngestionServiceImpl(
            TenantRepository tenantRepository,
            InstanceRepository instanceRepository,
            ApplicationRepository applicationRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            OfficeLocationRepository officeLocationRepository,
            GroupRepository groupRepository,
            RoleRepository roleRepository) {
        this.tenantRepository = tenantRepository;
        this.instanceRepository = instanceRepository;
        this.applicationRepository = applicationRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.officeLocationRepository = officeLocationRepository;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
    }
    
    @Override
    @Transactional
    public void ingestTransformedEntity(TransformedEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("TransformedEntity cannot be null");
        }
        
        log.info("Starting ingestion for entity: {}", entity.getEntityId());
        try {
            // 1. Upsert tenant
            UUID tenantId = upsertTenant(entity.getTenant());
            log.debug("Upserted tenant: {}", tenantId);
            
            // 2. Upsert instance
            UUID instanceId = upsertInstance(entity.getInstance());
            log.debug("Upserted instance: {}", instanceId);
            
            // 3. Upsert application (once per unique appId)
            if (entity.getApplication() != null) {
                String appId = upsertApplication(entity.getApplication());
                log.debug("Upserted application: {}", appId);
            }
            
            // 4. Upsert account (for each user)
            if (entity.getAccount() != null) {
                upsertAccount(entity.getAccount());
                log.debug("Upserted account: {}", entity.getAccount().getId());
            }
            
            // ID mappings to track original -> actual DB IDs for junction table links
            Map<UUID, UUID> departmentIdMapping = new HashMap<>();
            Map<UUID, UUID> officeLocationIdMapping = new HashMap<>();
            Map<UUID, UUID> groupIdMapping = new HashMap<>();
            Map<UUID, UUID> roleIdMapping = new HashMap<>();
            
            // 5. Upsert departments and track ID mappings
            for (DepartmentEntity dept : entity.getDepartments()) {
                UUID originalId = dept.getDepartmentId();
                UUID actualId = departmentRepository.upsert(dept);
                departmentIdMapping.put(originalId, actualId);
                dept.setDepartmentId(actualId); // Update entity with actual DB ID
                log.debug("Upserted department: {} (original: {})", actualId, originalId);
            }
            
            // 6. Upsert office locations and track ID mappings
            for (OfficeLocationEntity loc : entity.getOfficeLocations()) {
                UUID originalId = loc.getOfficeLocationId();
                UUID actualId = officeLocationRepository.upsert(loc);
                officeLocationIdMapping.put(originalId, actualId);
                loc.setOfficeLocationId(actualId); // Update entity with actual DB ID
                log.debug("Upserted office location: {} (original: {})", actualId, originalId);
            }
            
            // 7. Upsert groups and track ID mappings
            for (GroupEntity group : entity.getGroups()) {
                UUID originalId = group.getGroupId();
                UUID actualId = groupRepository.upsert(group);
                groupIdMapping.put(originalId, actualId);
                group.setGroupId(actualId); // Update entity with actual DB ID
                log.debug("Upserted group: {} (original: {})", actualId, originalId);
            }
            
            // 8. Upsert roles and track ID mappings
            for (RoleEntity role : entity.getRoles()) {
                UUID originalId = role.getRoleId();
                UUID actualId = roleRepository.upsert(role);
                roleIdMapping.put(originalId, actualId);
                role.setRoleId(actualId); // Update entity with actual DB ID
                log.debug("Upserted role: {} (original: {})", actualId, originalId);
            }
            
            // 9. Upsert user
            userRepository.upsert(entity.getUser());
            log.debug("Upserted user: {}", entity.getUser().getUserId());
            
            // 10. Upsert junction table links with corrected IDs
            upsertJunctionTableLinks(entity, departmentIdMapping, officeLocationIdMapping, groupIdMapping, roleIdMapping);
            
            log.info("Successfully ingested entity: {} - Tenant: {}, Instance: {}, Application: {}, Account: {}, User: {}, Groups: {}, Roles: {}", 
                entity.getEntityId(), tenantId, instanceId, 
                entity.getApplication() != null ? entity.getApplication().getId_() : "N/A",
                entity.getAccount() != null ? entity.getAccount().getId() : "N/A",
                entity.getUser().getUserId(), 
                entity.getGroups().size(), entity.getRoles().size());
                
        } catch (Exception e) {
            log.error("Error ingesting entity: {} - Error: {}", entity.getEntityId(), e.getMessage(), e);
            throw new RuntimeException("Failed to ingest entity: " + entity.getEntityId(), e);
        }
    }
    
    /**
     * Upsert tenant entity
     */
    private UUID upsertTenant(TenantEntity tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant entity cannot be null");
        }
        
        try {
            return tenantRepository.upsert(tenant);
        } catch (Exception e) {
            log.error("Failed to upsert tenant: {} - Error: {}", tenant.getId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Upsert instance entity
     */
    private UUID upsertInstance(InstanceEntity instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance entity cannot be null");
        }
        
        try {
            return instanceRepository.upsert(instance);
        } catch (Exception e) {
            log.error("Failed to upsert instance: {} - Error: {}", instance.getAppId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Upsert application entity
     */
    private String upsertApplication(ApplicationEntity application) {
        if (application == null) {
            throw new IllegalArgumentException("Application entity cannot be null");
        }
        
        try {
            return applicationRepository.upsert(application);
        } catch (Exception e) {
            log.error("Failed to upsert application: {} - Error: {}", application.getId_(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Upsert account entity
     */
    private void upsertAccount(AccountEntity account) {
        if (account == null) {
            throw new IllegalArgumentException("Account entity cannot be null");
        }
        
        try {
            accountRepository.upsert(account);
        } catch (Exception e) {
            log.error("Failed to upsert account: {} - Error: {}", account.getId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Upsert all junction table links.
     * Uses ID mappings to correct the foreign key references from original (transformer-generated)
     * IDs to actual database IDs returned by upsert operations.
     */
    private void upsertJunctionTableLinks(
            TransformedEntity entity,
            Map<UUID, UUID> departmentIdMapping,
            Map<UUID, UUID> officeLocationIdMapping,
            Map<UUID, UUID> groupIdMapping,
            Map<UUID, UUID> roleIdMapping) {
        
        // User-Department links - fix departmentId using mapping
        if (entity.getUserDepartments() != null) {
            for (UserDepartmentLink link : entity.getUserDepartments()) {
                UUID actualDeptId = departmentIdMapping.getOrDefault(link.getDepartmentId(), link.getDepartmentId());
                link.setDepartmentId(actualDeptId);
                upsertUserDepartmentLink(link);
            }
        }
        
        // User-Office Location links - fix officeLocationId using mapping
        if (entity.getUserOfficeLocations() != null) {
            for (UserOfficeLocationLink link : entity.getUserOfficeLocations()) {
                UUID actualLocId = officeLocationIdMapping.getOrDefault(link.getOfficeLocationId(), link.getOfficeLocationId());
                link.setOfficeLocationId(actualLocId);
                upsertUserOfficeLocationLink(link);
            }
        }
        
        // User-Group links - fix groupId using mapping
        if (entity.getUserGroups() != null) {
            for (UserGroupLink link : entity.getUserGroups()) {
                UUID actualGroupId = groupIdMapping.getOrDefault(link.getGroupId(), link.getGroupId());
                link.setGroupId(actualGroupId);
                upsertUserGroupLink(link);
            }
        }
        
        // User-Role links - fix roleId using mapping
        if (entity.getUserRoles() != null) {
            for (UserRoleLink link : entity.getUserRoles()) {
                UUID actualRoleId = roleIdMapping.getOrDefault(link.getRoleId(), link.getRoleId());
                link.setRoleId(actualRoleId);
                upsertUserRoleLink(link);
            }
        }
    }
    
    private void upsertUserDepartmentLink(UserDepartmentLink link) {
        try {
            departmentRepository.upsertUserDepartmentLink(link);
            log.debug("Upserted user-department link: User={}, Dept={}", 
                link.getUserId(), link.getDepartmentId());
        } catch (Exception e) {
            log.warn("Failed to upsert user-department link: {}", e.getMessage());
        }
    }
    
    private void upsertUserOfficeLocationLink(UserOfficeLocationLink link) {
        try {
            officeLocationRepository.upsertUserOfficeLocationLink(link);
            log.debug("Upserted user-office location link: User={}, Location={}", 
                link.getUserId(), link.getOfficeLocationId());
        } catch (Exception e) {
            log.warn("Failed to upsert user-office location link: {}", e.getMessage());
        }
    }
    
    private void upsertUserGroupLink(UserGroupLink link) {
        try {
            groupRepository.upsertUserGroupLink(link);
            log.debug("Upserted user-group link: User={}, Group={}", 
                link.getUserId(), link.getGroupId());
        } catch (Exception e) {
            log.warn("Failed to upsert user-group link: {}", e.getMessage());
        }
    }
    
    private void upsertUserRoleLink(UserRoleLink link) {
        try {
            roleRepository.upsertUserRoleLink(link);
            log.debug("Upserted user-role link: User={}, Role={}", 
                link.getUserId(), link.getRoleId());
        } catch (Exception e) {
            log.warn("Failed to upsert user-role link: {}", e.getMessage());
        }
    }
}

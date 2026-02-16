package com.quilr.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quilr.dto.EntityType;
import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;
import com.quilr.dto.VendorType;
import com.quilr.dto.entities.*;
import com.quilr.mapping.FieldMappingEngine;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic entity transformer that uses database-driven field mappings
 * Replaces vendor-specific static transformers like MicrosoftEntityTransformer
 */
@Component
@Log4j2
public class DynamicEntityTransformer implements EntityTransformer {
    
    private final FieldMappingEngine mappingEngine;
    private final ObjectMapper objectMapper;
    
    public DynamicEntityTransformer(FieldMappingEngine mappingEngine, ObjectMapper objectMapper) {
        this.mappingEngine = mappingEngine;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public TransformedEntity transformUsers(JsonNode payload, RawEntityMessage context) {
        log.debug("Transforming user entity dynamically - vendor: {}", context.getVendor());
        
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null for users transformation");
        }
        
        try {
            // Map single entities
            TenantEntity tenant = mappingEngine.mapEntity(payload, context, TenantEntity.class);
            InstanceEntity instance = mappingEngine.mapEntity(payload, context, InstanceEntity.class);
            ApplicationEntity application = mappingEngine.mapEntity(payload, context, ApplicationEntity.class);
            UserEntity user = mappingEngine.mapEntity(payload, context, UserEntity.class);
            AccountEntity account = mappingEngine.mapEntity(payload, context, AccountEntity.class);
            
            // Build extraInfo for user (complex JSONB fields)
            // Note: payload IS the data node (message.getData() returns the 'data' object directly)
            if (user != null) {
                Map<String, Object> extraInfo = buildUserExtraInfo(payload, user.getMail());
                user.setExtraInfo(extraInfo);
            }
            
            // Map array entities - departments and office locations (single values treated as arrays)
            List<DepartmentEntity> departments = extractDepartments(payload, tenant, context);
            List<OfficeLocationEntity> officeLocations = extractOfficeLocations(payload, tenant, context);
            
            // Map array entities - groups and roles (actual arrays)
            List<GroupEntity> groups = mappingEngine.mapEntityList(payload, context, GroupEntity.class, "data.groups");
            List<RoleEntity> roles = mappingEngine.mapEntityList(payload, context, RoleEntity.class, "data.roles");
            
            // Build extraInfo for groups
            // Note: payload IS the data node, so groups are at payload.groups not payload.data.groups
            if (payload.has("groups")) {
                JsonNode groupsArray = payload.get("groups");
                for (int i = 0; i < groups.size() && i < groupsArray.size(); i++) {
                    GroupEntity group = groups.get(i);
                    JsonNode groupNode = groupsArray.get(i);
                    Map<String, Object> extraInfo = buildGroupExtraInfo(groupNode);
                    group.setExtraInfo(extraInfo);
                }
            }
            
            // Build extraInfo for roles
            // Note: payload IS the data node, so roles are at payload.roles not payload.data.roles
            if (payload.has("roles")) {
                JsonNode rolesArray = payload.get("roles");
                for (int i = 0; i < roles.size() && i < rolesArray.size(); i++) {
                    RoleEntity role = roles.get(i);
                    JsonNode roleNode = rolesArray.get(i);
                    Map<String, Object> extraInfo = buildRoleExtraInfo(roleNode);
                    role.setExtraInfo(extraInfo);
                }
            }
            
            // Build junction table links
            List<UserDepartmentLink> userDepartments = buildUserDepartmentLinks(
                user.getUserId(), tenant.getTenantId(), departments
            );
            List<UserOfficeLocationLink> userOfficeLocations = buildUserOfficeLocationLinks(
                user.getUserId(), officeLocations
            );
            List<UserGroupLink> userGroups = buildUserGroupLinks(user.getUserId(), groups);
            List<UserRoleLink> userRoles = buildUserRoleLinks(user.getUserId(), roles);
            
            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("transformerVersion", "3.0-dynamic");
            metadata.put("sourceSystem", context.getVendor() + " API");
            metadata.put("transformedBy", this.getClass().getSimpleName());
            metadata.put("entitiesExtracted", Map.of(
                "tenant", 1,
                "instance", 1,
                "application", application != null ? 1 : 0,
                "account", account != null ? 1 : 0,
                "user", user != null ? 1 : 0,
                "departments", departments.size(),
                "officeLocations", officeLocations.size(),
                "groups", groups.size(),
                "roles", roles.size()
            ));
            
            // Build transformed entity
            TransformedEntity entity = TransformedEntity.builder()
                .entityId(user.getId())
                .entityType(EntityType.USERS)
                .vendor(VendorType.valueOf(context.getVendor().toUpperCase()))
                .transformedAt(Instant.now())
                .originalTimestamp(context.getTimestamp())
                .tenant(tenant)
                .instance(instance)
                .application(application)
                .account(account)
                .user(user)
                .departments(departments)
                .officeLocations(officeLocations)
                .groups(groups)
                .roles(roles)
                .userDepartments(userDepartments)
                .userOfficeLocations(userOfficeLocations)
                .userGroups(userGroups)
                .userRoles(userRoles)
                .metadata(metadata)
                .build();
            
            log.debug("Successfully transformed user dynamically - EntityId: {}, Groups: {}, Roles: {}", 
                entity.getEntityId(), groups.size(), roles.size());
            
            return entity;
            
        } catch (Exception e) {
            log.error("Error transforming user entity dynamically: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to transform user entity", e);
        }
    }
    
    @Override
    public TransformedEntity transformApps(JsonNode payload, RawEntityMessage context) {
        throw new UnsupportedOperationException(
            "Apps transformation not yet implemented for dynamic transformer"
        );
    }
    
    /**
     * Extract departments (single value treated as array)
     * Note: payload IS the data node (message.getData())
     */
    private List<DepartmentEntity> extractDepartments(JsonNode payload, TenantEntity tenant, RawEntityMessage context) {
        List<DepartmentEntity> departments = new ArrayList<>();
        
        if (payload.has("department")) {
            JsonNode deptNode = payload.get("department");
            if (!deptNode.isNull() && !deptNode.asText().isBlank()) {
                DepartmentEntity dept = mappingEngine.mapEntity(payload, context, DepartmentEntity.class);
                if (dept != null) {
                    departments.add(dept);
                }
            }
        }
        
        return departments;
    }
    
    /**
     * Extract office locations (single value treated as array)
     * Note: payload IS the data node (message.getData())
     */
    private List<OfficeLocationEntity> extractOfficeLocations(JsonNode payload, TenantEntity tenant, RawEntityMessage context) {
        List<OfficeLocationEntity> locations = new ArrayList<>();
        
        if (payload.has("officeLocation")) {
            JsonNode locNode = payload.get("officeLocation");
            if (!locNode.isNull() && !locNode.asText().isBlank()) {
                OfficeLocationEntity loc = mappingEngine.mapEntity(payload, context, OfficeLocationEntity.class);
                if (loc != null) {
                    locations.add(loc);
                }
            }
        }
        
        return locations;
    }
    
    /**
     * Build extraInfo JSONB for user
     */
    private Map<String, Object> buildUserExtraInfo(JsonNode dataNode, String primaryEmail) {
        Map<String, Object> extraInfo = new HashMap<>();
        
        // Contact information
        addIfPresent(extraInfo, "businessPhones", extractArrayField(dataNode, "businessPhones"));
        addIfPresent(extraInfo, "proxyAddresses", extractArrayField(dataNode, "proxyAddresses"));
        
        // Process otherMails - separate domain-matching from non-domain-matching
        List<String> otherMails = extractArrayField(dataNode, "otherMails");
        String primaryEmailDomain = null;
        if (primaryEmail != null && primaryEmail.contains("@")) {
            primaryEmailDomain = primaryEmail.split("@")[1];
        }
        
        String secondaryMailDomainMatch = null;
        String emailSecondary = null;
        
        if (!otherMails.isEmpty()) {
            for (String otherMail : otherMails) {
                if (otherMail != null && otherMail.contains("@")) {
                    String otherDomain = otherMail.split("@")[1];
                    if (primaryEmailDomain != null && otherDomain.equals(primaryEmailDomain)) {
                        if (secondaryMailDomainMatch == null) {
                            secondaryMailDomainMatch = otherMail.toLowerCase();
                        }
                    } else {
                        if (emailSecondary == null) {
                            emailSecondary = otherMail.toLowerCase();
                        }
                    }
                }
            }
        }
        
        addIfPresent(extraInfo, "secondaryMail", secondaryMailDomainMatch);
        addIfPresent(extraInfo, "emailSecondary", emailSecondary);
        if (!otherMails.isEmpty()) {
            addIfPresent(extraInfo, "otherMails", otherMails);
        }
        
        // Location information
        addIfPresent(extraInfo, "city", extractField(dataNode, "city"));
        addIfPresent(extraInfo, "state", extractField(dataNode, "state"));
        addIfPresent(extraInfo, "country", extractField(dataNode, "country"));
        addIfPresent(extraInfo, "streetAddress", extractField(dataNode, "streetAddress"));
        addIfPresent(extraInfo, "companyName", extractField(dataNode, "companyName"));
        
        // Security and authentication
        addIfPresent(extraInfo, "securityIdentifier", extractField(dataNode, "securityIdentifier"));
        addIfPresent(extraInfo, "passwordPolicies", extractField(dataNode, "passwordPolicies"));
        addIfPresent(extraInfo, "lastPasswordChangeDateTime", extractField(dataNode, "lastPasswordChangeDateTime"));
        
        // Other fields
        addIfPresent(extraInfo, "preferredLanguage", extractField(dataNode, "preferredLanguage"));
        addIfPresent(extraInfo, "mailNickname", extractField(dataNode, "mailNickname"));
        addIfPresent(extraInfo, "employeeId", extractField(dataNode, "employeeId"));
        
        // Complex objects
        if (dataNode.has("identities") && dataNode.get("identities").isArray()) {
            extraInfo.put("identities", extractJsonArray(dataNode.get("identities")));
        }
        if (dataNode.has("manager") && dataNode.get("manager").isArray()) {
            extraInfo.put("manager", extractJsonArray(dataNode.get("manager")));
        }
        
        // MFA/Registration details
        if (dataNode.has("registrationDetails") && dataNode.get("registrationDetails").isArray()) {
            JsonNode registrationDetailsArray = dataNode.get("registrationDetails");
            List<Map<String, Object>> registrationDetails = extractJsonArray(registrationDetailsArray);
            if (!registrationDetails.isEmpty()) {
                extraInfo.put("mfaDetails", registrationDetails.get(0));
                extraInfo.put("registrationDetails", registrationDetails);
            }
        } else if (dataNode.has("registrationDetails") && !dataNode.get("registrationDetails").isNull()) {
            Map<String, Object> registrationDetail = extractJsonObject(dataNode.get("registrationDetails"));
            if (!registrationDetail.isEmpty()) {
                extraInfo.put("mfaDetails", registrationDetail);
                extraInfo.put("registrationDetails", registrationDetail);
            }
        }
        
        // Add appId
        extraInfo.put("appId", "ee1b3219-7159-43f0-a5e0-8869de7bc4cd");
        
        return extraInfo.isEmpty() ? null : extraInfo;
    }
    
    /**
     * Build extraInfo for group
     */
    private Map<String, Object> buildGroupExtraInfo(JsonNode groupNode) {
        Map<String, Object> extraInfo = new HashMap<>();
        
        addIfPresent(extraInfo, "creationOptions", extractArrayField(groupNode, "creationOptions"));
        addIfPresent(extraInfo, "expirationDateTime", extractField(groupNode, "expirationDateTime"));
        addIfPresent(extraInfo, "resourceBehaviorOptions", extractArrayField(groupNode, "resourceBehaviorOptions"));
        addIfPresent(extraInfo, "resourceProvisioningOptions", extractArrayField(groupNode, "resourceProvisioningOptions"));
        addIfPresent(extraInfo, "onPremisesDomainName", extractField(groupNode, "onPremisesDomainName"));
        addIfPresent(extraInfo, "onPremisesNetBiosName", extractField(groupNode, "onPremisesNetBiosName"));
        addIfPresent(extraInfo, "onPremisesSamAccountName", extractField(groupNode, "onPremisesSamAccountName"));
        addIfPresent(extraInfo, "onPremisesSecurityIdentifier", extractField(groupNode, "onPremisesSecurityIdentifier"));
        addIfPresent(extraInfo, "onPremisesSyncEnabled", extractBooleanField(groupNode, "onPremisesSyncEnabled"));
        
        return extraInfo.isEmpty() ? null : extraInfo;
    }
    
    /**
     * Build extraInfo for role
     */
    private Map<String, Object> buildRoleExtraInfo(JsonNode roleNode) {
        Map<String, Object> extraInfo = new HashMap<>();
        
        if (roleNode.has("inheritsPermissionsFrom") && roleNode.get("inheritsPermissionsFrom").isArray()) {
            extraInfo.put("inheritsPermissionsFrom", extractJsonArray(roleNode.get("inheritsPermissionsFrom")));
        }
        if (roleNode.has("role_definition")) {
            extraInfo.put("role_definition", extractJsonObject(roleNode.get("role_definition")));
        }
        
        return extraInfo.isEmpty() ? null : extraInfo;
    }
    
    /**
     * Build user-department links
     */
    private List<UserDepartmentLink> buildUserDepartmentLinks(UUID userId, UUID tenantId, List<DepartmentEntity> departments) {
        return departments.stream()
            .map(dept -> UserDepartmentLink.builder()
                .userId(userId)
                .tenantId(tenantId)
                .departmentId(dept.getDepartmentId())
                .isPrimary(true)
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Build user-office location links
     */
    private List<UserOfficeLocationLink> buildUserOfficeLocationLinks(UUID userId, List<OfficeLocationEntity> locations) {
        return locations.stream()
            .map(loc -> UserOfficeLocationLink.builder()
                .userId(userId)
                .officeLocationId(loc.getOfficeLocationId())
                .isPrimary(true)
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Build user-group links
     */
    private List<UserGroupLink> buildUserGroupLinks(UUID userId, List<GroupEntity> groups) {
        return groups.stream()
            .map(group -> UserGroupLink.builder()
                .userId(userId)
                .groupId(group.getGroupId())
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Build user-role links
     */
    private List<UserRoleLink> buildUserRoleLinks(UUID userId, List<RoleEntity> roles) {
        return roles.stream()
            .map(role -> UserRoleLink.builder()
                .userId(userId)
                .roleId(role.getRoleId())
                .assignmentType(role.getAssignmentType())
                .build())
            .collect(Collectors.toList());
    }
    
    // ===== Helper Methods =====
    
    private String extractField(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode.isNull() ? null : fieldNode.asText();
    }
    
    private Boolean extractBooleanField(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode.isNull() ? null : fieldNode.asBoolean();
    }
    
    private List<String> extractArrayField(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || !node.get(fieldName).isArray()) {
            return new ArrayList<>();
        }
        
        JsonNode arrayNode = node.get(fieldName);
        List<String> result = new ArrayList<>();
        for (JsonNode element : arrayNode) {
            if (!element.isNull()) {
                result.add(element.asText());
            }
        }
        return result;
    }
    
    private List<Map<String, Object>> extractJsonArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode element : arrayNode) {
            result.add(extractJsonObject(element));
        }
        return result;
    }
    
    private Map<String, Object> extractJsonObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                result.put(entry.getKey(), value.asText());
            } else if (value.isBoolean()) {
                result.put(entry.getKey(), value.asBoolean());
            } else if (value.isNumber()) {
                result.put(entry.getKey(), value.asLong());
            } else if (value.isArray()) {
                result.put(entry.getKey(), extractJsonArray(value));
            } else if (value.isObject()) {
                result.put(entry.getKey(), extractJsonObject(value));
            }
        });
        return result;
    }
    
    private void addIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            if (value instanceof String && ((String) value).isBlank()) {
                return;
            }
            if (value instanceof List && ((List<?>) value).isEmpty()) {
                return;
            }
            map.put(key, value);
        }
    }
}

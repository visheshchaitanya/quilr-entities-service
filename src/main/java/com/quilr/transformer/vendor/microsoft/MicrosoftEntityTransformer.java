package com.quilr.transformer.vendor.microsoft;

import com.fasterxml.jackson.databind.JsonNode;
import com.quilr.dto.EntityType;
import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;
import com.quilr.dto.VendorType;
import com.quilr.dto.entities.*;
import com.quilr.transformer.EntityTransformer;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Microsoft-specific entity transformer.
 * Handles transformation of Microsoft Graph API user and app data.
 * 
 * Extracts all entities from payload:
 * - Tenant, Instance, User
 * - Departments, Office Locations
 * - Groups, Roles
 * - Junction table links
 */
@Component
@Log4j2
public class MicrosoftEntityTransformer implements EntityTransformer {
    
    @Override
    public TransformedEntity transformUsers(JsonNode payload, RawEntityMessage context) {
        log.debug("Transforming Microsoft user entity");
        
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null for Microsoft users transformation");
        }
        
        try {
            // Extract tenant and instance from context (root level fields)
            TenantEntity tenant = extractTenantEntity(context);
            InstanceEntity instance = extractInstanceEntity(context, tenant.getTenantId());
            
            // Extract application entity
            ApplicationEntity application = extractApplicationEntity(context);
            
            // The payload passed here is already the 'data' node
            JsonNode dataNode = payload;
            
            // Extract user entity
            UserEntity user = extractUserEntity(dataNode, tenant.getTenantId(), instance.getInstanceId());
            
            // Extract account entity
            AccountEntity account = extractAccountEntity(user, tenant.getTenantId(), application.getId_(), context);
            
            // Extract related entities
            List<DepartmentEntity> departments = extractDepartments(dataNode, tenant.getTenantId());
            List<OfficeLocationEntity> officeLocations = extractOfficeLocations(dataNode, tenant.getTenantId());
            List<GroupEntity> groups = extractGroups(dataNode, tenant.getTenantId());
            List<RoleEntity> roles = extractRoles(dataNode, tenant.getTenantId());
            
            // Build junction table links
            List<UserDepartmentLink> userDepartments = buildUserDepartmentLinks(user.getUserId(), tenant.getTenantId(), departments);
            List<UserOfficeLocationLink> userOfficeLocations = buildUserOfficeLocationLinks(user.getUserId(), officeLocations);
            List<UserGroupLink> userGroups = buildUserGroupLinks(user.getUserId(), groups);
            List<UserRoleLink> userRoles = buildUserRoleLinks(user.getUserId(), roles);
            
            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("transformerVersion", "2.1-with-app-account");
            metadata.put("sourceSystem", "Microsoft Graph API");
            metadata.put("transformedBy", this.getClass().getSimpleName());
            metadata.put("entitiesExtracted", Map.of(
                "tenant", 1,
                "instance", 1,
                "application", 1,
                "account", 1,
                "user", 1,
                "departments", departments.size(),
                "officeLocations", officeLocations.size(),
                "groups", groups.size(),
                "roles", roles.size()
            ));
            
            // Build transformed entity
            TransformedEntity entity = TransformedEntity.builder()
                .entityId(user.getId())
                .entityType(EntityType.USERS)
                .vendor(VendorType.MICROSOFT)
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
            
            log.debug("Successfully transformed Microsoft user - EntityId: {}, Groups: {}, Roles: {}, Application: {}, Account: {}", 
                entity.getEntityId(), groups.size(), roles.size(), application.getId_(), account.getId());
            
            return entity;
            
        } catch (Exception e) {
            log.error("Error transforming Microsoft user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to transform Microsoft user entity", e);
        }
    }
    
    @Override
    public TransformedEntity transformApps(JsonNode payload, RawEntityMessage context) {
        // Apps transformation not yet implemented
        throw new UnsupportedOperationException(
            "Microsoft apps transformation not yet implemented. " +
            "This will be added in detailed implementation phase."
        );
    }
    
    /**
     * Extract tenant entity from context (root level fields)
     */
    private TenantEntity extractTenantEntity(RawEntityMessage context) {
        String tenantIdStr = context.getTenant();
        String subscriberIdStr = context.getSubscriber();
        
        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : UUID.randomUUID();
        UUID subscriberId = subscriberIdStr != null ? UUID.fromString(subscriberIdStr) : null;
        
        return TenantEntity.builder()
            .tenantId(tenantId)
            .id(tenantIdStr)
            .subscriberId(subscriberId)
            .build();
    }
    
    /**
     * Extract instance entity from context (root level fields)
     */
    private InstanceEntity extractInstanceEntity(RawEntityMessage context, UUID tenantId) {
        String instanceIdStr = context.getInstanceId();
        String appDomain = context.getDomain();
        
        UUID instanceId = instanceIdStr != null ? UUID.fromString(instanceIdStr) : null;
        
        return InstanceEntity.builder()
            .instanceId(instanceId)
            .tenantId(tenantId)
            .appId(appDomain)
            .build();
    }
    
    /**
     * Extract user entity from data node
     */
    private UserEntity extractUserEntity(JsonNode dataNode, UUID tenantId, UUID instanceId) {
        // Extract and normalize email fields (must be lowercase)
        String mail = extractField(dataNode, "mail");
        if (mail != null) {
            mail = mail.toLowerCase();
        }
        
        String userPrincipalName = extractField(dataNode, "userPrincipalName");
        if (userPrincipalName != null) {
            userPrincipalName = userPrincipalName.toLowerCase();
        }
        
        // Determine identifier and email using Python's fallback logic
        String mailValue = mail != null ? mail : userPrincipalName;
        String identifier = mailValue != null ? mailValue : userPrincipalName;
        if (identifier == null) {
            identifier = extractField(dataNode, "id");
        }
        String email = mailValue != null ? mailValue : identifier;
        
        // Extract user type and normalize to lowercase
        String userType = extractField(dataNode, "userType");
        
        // Determine account state
        Boolean accountEnabled = extractBooleanField(dataNode, "accountEnabled");
        
        // Check if user is guest
        boolean isGuest = StringUtils.isNotEmpty(userType) && "guest".equalsIgnoreCase(userType);
        
        // Determine extension deployment status
        String extensionDeploymentStatus = (accountEnabled != null && accountEnabled && !isGuest) 
            ? "Ready to Deploy" 
            : "Excluded";
        
        // Extract business phone (preferred from array)
        String businessPhone = preferredBusinessPhone(extractArrayField(dataNode, "businessPhones"));
        
        // Extract last login time from signInActivity
        Instant lastLoginTime = null;
        if (dataNode.has("signInActivity") && !dataNode.get("signInActivity").isNull()) {
            JsonNode signInActivity = dataNode.get("signInActivity");
            lastLoginTime = parseInstant(extractField(signInActivity, "lastSignInDateTime"));
        }
        
        // Build extraInfo with additional fields including otherMails processing
        Map<String, Object> extraInfo = buildExtraInfo(dataNode, email);
        
        // user_id must be unique per tenant (same Microsoft user can exist in multiple tenants)
        // Generate deterministic UUID from tenant_id + Microsoft user ID
        String microsoftUserId = extractField(dataNode, "id");
        UUID userId = UUID.nameUUIDFromBytes((tenantId.toString() + ":" + microsoftUserId).getBytes());
        
        return UserEntity.builder()
            .userId(userId)
            .tenantId(tenantId)
            .instanceId(instanceId)
            .id(microsoftUserId)
            .displayName(extractField(dataNode, "displayName"))
            .givenName(extractField(dataNode, "givenName"))
            .surname(extractField(dataNode, "surname"))
            .mail(mail)
            .userPrincipalName(userPrincipalName)
            .mobilePhone(extractField(dataNode, "mobilePhone"))
            .jobTitle(extractField(dataNode, "jobTitle"))
            .employeeType(extractField(dataNode, "employeeType"))
            .employeeHireDate(parseDate(extractField(dataNode, "employeeHireDate")))
            .terminationDate(parseDate(extractField(dataNode, "employeeLeaveDateTime")))
            .accountEnabled(accountEnabled)
            .userType(userType)
            .extensionDeploymentStatus(extensionDeploymentStatus)
            .userCreationTime(parseInstant(extractField(dataNode, "createdDateTime")))
            .userLastLoginTime(lastLoginTime)
            .extraInfo(extraInfo)
            .build();
    }
    
    /**
     * Extract departments from data node
     */
    private List<DepartmentEntity> extractDepartments(JsonNode dataNode, UUID tenantId) {
        List<DepartmentEntity> departments = new ArrayList<>();
        
        String departmentName = extractField(dataNode, "department");
        if (departmentName != null && !departmentName.isBlank()) {
            UUID departmentId = UUID.nameUUIDFromBytes((tenantId.toString() + ":dept:" + departmentName).getBytes());
            departments.add(DepartmentEntity.builder()
                .departmentId(departmentId)
                .tenantId(tenantId)
                .id(departmentName)
                .name(departmentName)
                .build());
        }
        
        return departments;
    }
    
    /**
     * Extract office locations from data node
     */
    private List<OfficeLocationEntity> extractOfficeLocations(JsonNode dataNode, UUID tenantId) {
        List<OfficeLocationEntity> locations = new ArrayList<>();
        
        String officeLocation = extractField(dataNode, "officeLocation");
        if (officeLocation != null && !officeLocation.isBlank()) {
            UUID officeLocationId = UUID.nameUUIDFromBytes((tenantId.toString() + ":office:" + officeLocation).getBytes());
            locations.add(OfficeLocationEntity.builder()
                .officeLocationId(officeLocationId)
                .tenantId(tenantId)
                .id(officeLocation)
                .name(officeLocation)
                .build());
        }
        
        return locations;
    }
    
    /**
     * Extract groups from data node
     */
    private List<GroupEntity> extractGroups(JsonNode dataNode, UUID tenantId) {
        List<GroupEntity> groups = new ArrayList<>();
        
        if (!dataNode.has("groups") || !dataNode.get("groups").isArray()) {
            return groups;
        }
        
        JsonNode groupsArray = dataNode.get("groups");
        for (JsonNode groupNode : groupsArray) {
            try {
                String groupMsId = extractField(groupNode, "id");
                UUID groupId = UUID.nameUUIDFromBytes((tenantId.toString() + ":group:" + groupMsId).getBytes());
                List<String> groupTypes = extractArrayField(groupNode, "groupTypes");
                
                GroupEntity group = GroupEntity.builder()
                    .groupId(groupId)
                    .tenantId(tenantId)
                    .id(groupMsId)
                    .displayName(extractField(groupNode, "displayName"))
                    .mail(extractField(groupNode, "mail"))
                    .mailEnabled(extractBooleanField(groupNode, "mailEnabled"))
                    .securityEnabled(extractBooleanField(groupNode, "securityEnabled"))
                    .groupTypes(groupTypes)
                    .createdDateTime(parseInstant(extractField(groupNode, "createdDateTime")))
                    .description(extractField(groupNode, "description"))
                    .visibility(extractField(groupNode, "visibility"))
                    .classification(extractField(groupNode, "classification"))
                    .mailNickname(extractField(groupNode, "mailNickname"))
                    .membershipRule(extractField(groupNode, "membershipRule"))
                    .membershipRuleProcessingState(extractField(groupNode, "membershipRuleProcessingState"))
                    .preferredDataLocation(extractField(groupNode, "preferredDataLocation"))
                    .preferredLanguage(extractField(groupNode, "preferredLanguage"))
                    .renewedDateTime(parseInstant(extractField(groupNode, "renewedDateTime")))
                    .theme(extractField(groupNode, "theme"))
                    .uniqueName(extractField(groupNode, "uniqueName"))
                    .isAssignableToRole(extractBooleanField(groupNode, "isAssignableToRole"))
                    .extraInfo(buildGroupExtraInfo(groupNode))
                    .build();
                
                groups.add(group);
            } catch (Exception e) {
                log.warn("Failed to extract group: {}", e.getMessage());
            }
        }
        
        return groups;
    }
    
    /**
     * Extract roles from data node
     */
    private List<RoleEntity> extractRoles(JsonNode dataNode, UUID tenantId) {
        List<RoleEntity> roles = new ArrayList<>();
        
        if (!dataNode.has("roles") || !dataNode.get("roles").isArray()) {
            return roles;
        }
        
        JsonNode rolesArray = dataNode.get("roles");
        for (JsonNode roleNode : rolesArray) {
            try {
                String roleMsId = extractField(roleNode, "id");
                UUID roleId = UUID.nameUUIDFromBytes((tenantId.toString() + ":role:" + roleMsId).getBytes());
                RoleEntity role = RoleEntity.builder()
                    .roleId(roleId)
                    .tenantId(tenantId)
                    .id(roleMsId)
                    .displayName(extractField(roleNode, "displayName"))
                    .description(extractField(roleNode, "description"))
                    .isBuiltIn(extractBooleanField(roleNode, "isBuiltIn"))
                    .isEnabled(extractBooleanField(roleNode, "isEnabled"))
                    .isPrivileged(extractBooleanField(roleNode, "isPrivileged"))
                    .roleTemplateId(extractField(roleNode, "roleTemplateId"))
                    .assignmentType(extractField(roleNode, "assignmentType"))
                    .extraInfo(buildRoleExtraInfo(roleNode))
                    .build();
                
                roles.add(role);
            } catch (Exception e) {
                log.warn("Failed to extract role: {}", e.getMessage());
            }
        }
        
        return roles;
    }
    
    /**
     * Build extra_info JSONB for user
     */
    private Map<String, Object> buildExtraInfo(JsonNode dataNode, String primaryEmail) {
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
                        // Domain-matching email → secondary_mail node
                        if (secondaryMailDomainMatch == null) {
                            secondaryMailDomainMatch = otherMail.toLowerCase();
                        }
                    } else {
                        // Non-domain-matching email → emailsecondary node
                        if (emailSecondary == null) {
                            emailSecondary = otherMail.toLowerCase();
                        }
                    }
                }
            }
        }
        
        // Add separated email fields
        addIfPresent(extraInfo, "secondaryMail", secondaryMailDomainMatch);
        addIfPresent(extraInfo, "emailSecondary", emailSecondary);
        
        // Store original otherMails for reference
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
        
        // Extract MFA/Registration details
        if (dataNode.has("registrationDetails") && dataNode.get("registrationDetails").isArray()) {
            JsonNode registrationDetailsArray = dataNode.get("registrationDetails");
            List<Map<String, Object>> registrationDetails = extractJsonArray(registrationDetailsArray);
            
            // Extract first element or entire array based on Python logic
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
        
        // Add appId (hardcoded as per Python code)
        extraInfo.put("appId", "ee1b3219-7159-43f0-a5e0-8869de7bc4cd");
        
        return extraInfo.isEmpty() ? null : extraInfo;
    }
    
    /**
     * Build extra_info for group
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
     * Build extra_info for role
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
    
    /**
     * Extract preferred business phone from array (first non-empty entry)
     */
    private String preferredBusinessPhone(List<String> businessPhones) {
        if (businessPhones == null || businessPhones.isEmpty()) {
            return null;
        }
        for (String phone : businessPhones) {
            if (phone != null && !phone.isBlank()) {
                return phone;
            }
        }
        return null;
    }
    
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
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }
    
    private Instant parseInstant(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(dateTimeStr);
        } catch (Exception e) {
            log.warn("Failed to parse instant: {}", dateTimeStr);
            return null;
        }
    }
    
    /**
     * Extract application entity from context (root level fields)
     * Uses hardcoded appId for Microsoft Entra ID
     */
    private ApplicationEntity extractApplicationEntity(RawEntityMessage context) {
        String domain = context.getDomain();
        String appId = "ee1b3219-7159-43f0-a5e0-8869de7bc4cd"; // Hardcoded Microsoft Entra ID appId
        
        return ApplicationEntity.builder()
            .id_(appId)
            .domain(domain)
            .newApp(false)
            .globalSyncAllowed(false)
            .build();
    }
    
    /**
     * Extract account entity from user and context
     * Constructs account ID as lower(userPrincipalName)_appId
     */
    private AccountEntity extractAccountEntity(UserEntity user, UUID tenantId, String appId, RawEntityMessage context) {
        // Construct account ID: lower(userMail)_appId with fallback to mail
        String upn = user.getUserPrincipalName();
        String mail = user.getMail();
        String identifier = mail != null ? mail : upn;
        
        if (StringUtils.isEmpty(identifier)) {
            log.warn("Both userPrincipalName and mail are null for user: {}", user.getId());
            identifier = user.getId(); // Fallback to user ID
        }
        
        String accountId = identifier.toLowerCase() + "_" + appId;
        
        return AccountEntity.builder()
            .id(accountId)
            .tenantId(tenantId)
            .email(mail)
            .appName("Microsoft Entra ID")
            .appId(appId)
            .microsoftId(null)
            .creationTime(user.getUserCreationTime())
            .build();
    }
}

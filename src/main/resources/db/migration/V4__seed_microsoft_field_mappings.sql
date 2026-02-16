-- Seed field_mappings for Microsoft user entities
-- Extracted from MicrosoftEntityTransformer.java

-- ============================================================================
-- TENANT ENTITY MAPPINGS
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'tenant', 'id', '$.tenant', 'STRING', NULL, NULL, true, 1),
('Microsoft', 'users', 'tenant', 'tenantId', '$.tenant', 'UUID', 'uuid_from_string', '{"field": "tenant"}', true, 2),
('Microsoft', 'users', 'tenant', 'subscriberId', '$.subscriber', 'UUID', 'uuid_from_string', '{"field": "subscriber"}', false, 3)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- INSTANCE ENTITY MAPPINGS
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'instance', 'instanceId', '$.instance_id', 'UUID', 'uuid_from_string', '{"field": "instance_id"}', true, 1),
('Microsoft', 'users', 'instance', 'appId', '$.domain', 'STRING', NULL, NULL, false, 2)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- APPLICATION ENTITY MAPPINGS
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, default_value, required, priority) VALUES
('Microsoft', 'users', 'application', 'id_', NULL, 'STRING', NULL, 'ee1b3219-7159-43f0-a5e0-8869de7bc4cd', true, 1),
('Microsoft', 'users', 'application', 'domain', '$.domain', 'STRING', NULL, NULL, false, 2),
('Microsoft', 'users', 'application', 'newApp', NULL, 'BOOLEAN', NULL, 'false', false, 3),
('Microsoft', 'users', 'application', 'globalSyncAllowed', NULL, 'BOOLEAN', NULL, 'false', false, 4)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- USER ENTITY MAPPINGS
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, fallback_paths, data_type, transform, transform_args, required, priority) VALUES
-- Core ID fields
('Microsoft', 'users', 'user', 'id', '$.data.id', NULL, 'STRING', NULL, NULL, true, 1),
('Microsoft', 'users', 'user', 'userId', '$.data.id', NULL, 'UUID', 'uuid_from_bytes', '{"fields": ["tenant", "data.id"]}', true, 2),

-- Name fields
('Microsoft', 'users', 'user', 'displayName', '$.data.displayName', NULL, 'STRING', NULL, NULL, false, 10),
('Microsoft', 'users', 'user', 'givenName', '$.data.givenName', NULL, 'STRING', NULL, NULL, false, 11),
('Microsoft', 'users', 'user', 'surname', '$.data.surname', NULL, 'STRING', NULL, NULL, false, 12),

-- Email fields (with lowercase transform)
('Microsoft', 'users', 'user', 'mail', '$.data.mail', NULL, 'STRING', 'lowercase', NULL, false, 20),
('Microsoft', 'users', 'user', 'userPrincipalName', '$.data.userPrincipalName', NULL, 'STRING', 'lowercase', NULL, false, 21),

-- Contact fields
('Microsoft', 'users', 'user', 'mobilePhone', '$.data.mobilePhone', NULL, 'STRING', NULL, NULL, false, 30),

-- Employment fields
('Microsoft', 'users', 'user', 'jobTitle', '$.data.jobTitle', NULL, 'STRING', NULL, NULL, false, 40),
('Microsoft', 'users', 'user', 'employeeType', '$.data.employeeType', NULL, 'STRING', NULL, NULL, false, 41),
('Microsoft', 'users', 'user', 'employeeHireDate', '$.data.employeeHireDate', NULL, 'DATE', 'parse_iso_date', NULL, false, 42),
('Microsoft', 'users', 'user', 'terminationDate', '$.data.employeeLeaveDateTime', NULL, 'DATE', 'parse_iso_date', NULL, false, 43),

-- Account status fields
('Microsoft', 'users', 'user', 'accountEnabled', '$.data.accountEnabled', NULL, 'BOOLEAN', NULL, NULL, false, 50),
('Microsoft', 'users', 'user', 'userType', '$.data.userType', NULL, 'STRING', NULL, NULL, false, 51),
('Microsoft', 'users', 'user', 'extensionDeploymentStatus', '$.data.accountEnabled', NULL, 'STRING', 'extension_deployment_status', '{"userTypeField": "data.userType"}', false, 52),

-- Timestamp fields
('Microsoft', 'users', 'user', 'userCreationTime', '$.data.createdDateTime', NULL, 'TIMESTAMP', 'parse_iso_date', NULL, false, 60),
('Microsoft', 'users', 'user', 'userLastLoginTime', '$.data.signInActivity.lastSignInDateTime', NULL, 'TIMESTAMP', 'parse_iso_date', NULL, false, 61)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- ACCOUNT ENTITY MAPPINGS
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, default_value, required, priority) VALUES
('Microsoft', 'users', 'account', 'id', '$.data.mail', 'STRING', 'build_account_id', '{"emailField": "data.mail", "upnField": "data.userPrincipalName", "userIdField": "data.id", "appId": "ee1b3219-7159-43f0-a5e0-8869de7bc4cd"}', NULL, true, 1),
('Microsoft', 'users', 'account', 'email', '$.data.mail', 'STRING', 'lowercase', NULL, NULL, false, 2),
('Microsoft', 'users', 'account', 'appName', NULL, 'STRING', NULL, NULL, 'Microsoft Entra ID', false, 3),
('Microsoft', 'users', 'account', 'appId', NULL, 'STRING', NULL, NULL, 'ee1b3219-7159-43f0-a5e0-8869de7bc4cd', false, 4),
('Microsoft', 'users', 'account', 'creationTime', '$.data.createdDateTime', 'TIMESTAMP', 'parse_iso_date', NULL, NULL, false, 5)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- GROUP ENTITY MAPPINGS (ARRAY ENTITY - uses [*] notation)
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
-- Core ID fields
('Microsoft', 'users', 'group', 'id', '$.data.groups[*].id', 'STRING', NULL, NULL, true, 1),
('Microsoft', 'users', 'group', 'groupId', '$.data.groups[*].id', 'UUID', 'uuid_from_bytes', '{"fields": ["tenant", "id"], "prefix": "group"}', true, 2),

-- Basic fields
('Microsoft', 'users', 'group', 'displayName', '$.data.groups[*].displayName', 'STRING', NULL, NULL, false, 10),
('Microsoft', 'users', 'group', 'mail', '$.data.groups[*].mail', 'STRING', NULL, NULL, false, 11),
('Microsoft', 'users', 'group', 'description', '$.data.groups[*].description', 'STRING', NULL, NULL, false, 12),

-- Boolean fields
('Microsoft', 'users', 'group', 'mailEnabled', '$.data.groups[*].mailEnabled', 'BOOLEAN', NULL, NULL, false, 20),
('Microsoft', 'users', 'group', 'securityEnabled', '$.data.groups[*].securityEnabled', 'BOOLEAN', NULL, NULL, false, 21),
('Microsoft', 'users', 'group', 'isAssignableToRole', '$.data.groups[*].isAssignableToRole', 'BOOLEAN', NULL, NULL, false, 22),

-- Array field
('Microsoft', 'users', 'group', 'groupTypes', '$.data.groups[*].groupTypes', 'ARRAY', NULL, NULL, false, 30),

-- Timestamp fields
('Microsoft', 'users', 'group', 'createdDateTime', '$.data.groups[*].createdDateTime', 'TIMESTAMP', 'parse_iso_date', NULL, false, 40),
('Microsoft', 'users', 'group', 'renewedDateTime', '$.data.groups[*].renewedDateTime', 'TIMESTAMP', 'parse_iso_date', NULL, false, 41),

-- Additional string fields
('Microsoft', 'users', 'group', 'visibility', '$.data.groups[*].visibility', 'STRING', NULL, NULL, false, 50),
('Microsoft', 'users', 'group', 'classification', '$.data.groups[*].classification', 'STRING', NULL, NULL, false, 51),
('Microsoft', 'users', 'group', 'mailNickname', '$.data.groups[*].mailNickname', 'STRING', NULL, NULL, false, 52),
('Microsoft', 'users', 'group', 'membershipRule', '$.data.groups[*].membershipRule', 'STRING', NULL, NULL, false, 53),
('Microsoft', 'users', 'group', 'membershipRuleProcessingState', '$.data.groups[*].membershipRuleProcessingState', 'STRING', NULL, NULL, false, 54),
('Microsoft', 'users', 'group', 'preferredDataLocation', '$.data.groups[*].preferredDataLocation', 'STRING', NULL, NULL, false, 55),
('Microsoft', 'users', 'group', 'preferredLanguage', '$.data.groups[*].preferredLanguage', 'STRING', NULL, NULL, false, 56),
('Microsoft', 'users', 'group', 'theme', '$.data.groups[*].theme', 'STRING', NULL, NULL, false, 57),
('Microsoft', 'users', 'group', 'uniqueName', '$.data.groups[*].uniqueName', 'STRING', NULL, NULL, false, 58)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- ROLE ENTITY MAPPINGS (ARRAY ENTITY - uses [*] notation)
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
-- Core ID fields
('Microsoft', 'users', 'role', 'id', '$.data.roles[*].id', 'STRING', NULL, NULL, true, 1),
('Microsoft', 'users', 'role', 'roleId', '$.data.roles[*].id', 'UUID', 'uuid_from_bytes', '{"fields": ["tenant", "id"], "prefix": "role"}', true, 2),

-- Basic fields
('Microsoft', 'users', 'role', 'displayName', '$.data.roles[*].displayName', 'STRING', NULL, NULL, false, 10),
('Microsoft', 'users', 'role', 'description', '$.data.roles[*].description', 'STRING', NULL, NULL, false, 11),

-- Boolean fields
('Microsoft', 'users', 'role', 'isBuiltIn', '$.data.roles[*].isBuiltIn', 'BOOLEAN', NULL, NULL, false, 20),
('Microsoft', 'users', 'role', 'isEnabled', '$.data.roles[*].isEnabled', 'BOOLEAN', NULL, NULL, false, 21),
('Microsoft', 'users', 'role', 'isPrivileged', '$.data.roles[*].isPrivileged', 'BOOLEAN', NULL, NULL, false, 22),

-- Additional fields
('Microsoft', 'users', 'role', 'roleTemplateId', '$.data.roles[*].roleTemplateId', 'STRING', NULL, NULL, false, 30),
('Microsoft', 'users', 'role', 'assignmentType', '$.data.roles[*].assignmentType', 'STRING', NULL, NULL, false, 31)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- DEPARTMENT ENTITY MAPPINGS
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'department', 'id', '$.data.department', 'STRING', NULL, NULL, true, 1),
('Microsoft', 'users', 'department', 'departmentId', '$.data.department', 'UUID', 'uuid_from_bytes', '{"fields": ["tenant", "data.department"], "prefix": "dept"}', true, 2),
('Microsoft', 'users', 'department', 'name', '$.data.department', 'STRING', NULL, NULL, false, 3)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- OFFICE LOCATION ENTITY MAPPINGS
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'office_location', 'id', '$.data.officeLocation', 'STRING', NULL, NULL, true, 1),
('Microsoft', 'users', 'office_location', 'officeLocationId', '$.data.officeLocation', 'UUID', 'uuid_from_bytes', '{"fields": ["tenant", "data.officeLocation"], "prefix": "office"}', true, 2),
('Microsoft', 'users', 'office_location', 'name', '$.data.officeLocation', 'STRING', NULL, NULL, false, 3)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

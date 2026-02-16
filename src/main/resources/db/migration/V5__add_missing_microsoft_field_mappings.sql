-- Add missing field mappings for Microsoft user entities
-- This migration adds all fields that were present in the old MicrosoftEntityTransformer
-- but were missing from the initial V4 seed

-- ============================================================================
-- INSTANCE ENTITY - Add tenantId
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'instance', 'tenantId', '$.tenant', 'UUID', 'uuid_from_string', '{"field": "tenant"}', true, 2)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- USER ENTITY - Add missing fields
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, fallback_paths, data_type, transform, transform_args, required, priority) VALUES
-- Core ID fields
('Microsoft', 'users', 'user', 'tenantId', '$.tenant', NULL, 'UUID', 'uuid_from_string', '{"field": "tenant"}', true, 3),
('Microsoft', 'users', 'user', 'instanceId', '$.instance_id', NULL, 'UUID', 'uuid_from_string', '{"field": "instance_id"}', true, 4),

-- Email fields (with lowercase transform and fallbacks)
('Microsoft', 'users', 'user', 'identifier', '$.data.mail', '["$.data.userPrincipalName", "$.data.id"]', 'STRING', 'lowercase', NULL, false, 22),
('Microsoft', 'users', 'user', 'email', '$.data.mail', '["$.data.userPrincipalName", "$.data.id"]', 'STRING', 'lowercase', NULL, false, 23),
('Microsoft', 'users', 'user', 'otherMails', '$.data.otherMails', NULL, 'ARRAY', NULL, NULL, false, 24),
('Microsoft', 'users', 'user', 'proxyAddresses', '$.data.proxyAddresses', NULL, 'ARRAY', NULL, NULL, false, 25),

-- Contact fields
('Microsoft', 'users', 'user', 'businessPhone', '$.data.businessPhones[0]', NULL, 'STRING', NULL, NULL, false, 31),

-- Employment fields
('Microsoft', 'users', 'user', 'employeeId', '$.data.employeeId', NULL, 'STRING', NULL, NULL, false, 44),
('Microsoft', 'users', 'user', 'companyName', '$.data.companyName', NULL, 'STRING', NULL, NULL, false, 45),
('Microsoft', 'users', 'user', 'department', '$.data.department', NULL, 'STRING', NULL, NULL, false, 46),
('Microsoft', 'users', 'user', 'officeLocation', '$.data.officeLocation', NULL, 'STRING', NULL, NULL, false, 47),
('Microsoft', 'users', 'user', 'costCenter', '$.data.costCenter', NULL, 'STRING', NULL, NULL, false, 48),
('Microsoft', 'users', 'user', 'division', '$.data.division', NULL, 'STRING', NULL, NULL, false, 49),

-- Location fields
('Microsoft', 'users', 'user', 'city', '$.data.city', NULL, 'STRING', NULL, NULL, false, 70),
('Microsoft', 'users', 'user', 'state', '$.data.state', NULL, 'STRING', NULL, NULL, false, 71),
('Microsoft', 'users', 'user', 'country', '$.data.country', NULL, 'STRING', NULL, NULL, false, 72),
('Microsoft', 'users', 'user', 'postalCode', '$.data.postalCode', NULL, 'STRING', NULL, NULL, false, 73),
('Microsoft', 'users', 'user', 'streetAddress', '$.data.streetAddress', NULL, 'STRING', NULL, NULL, false, 74),
('Microsoft', 'users', 'user', 'usageLocation', '$.data.usageLocation', NULL, 'STRING', NULL, NULL, false, 75),

-- Manager field
('Microsoft', 'users', 'user', 'manager', '$.data.manager', NULL, 'STRING', NULL, NULL, false, 80),

-- On-premises sync fields
('Microsoft', 'users', 'user', 'onPremisesImmutableId', '$.data.onPremisesImmutableId', NULL, 'STRING', NULL, NULL, false, 90),
('Microsoft', 'users', 'user', 'onPremisesSamAccountName', '$.data.onPremisesSamAccountName', NULL, 'STRING', NULL, NULL, false, 91),
('Microsoft', 'users', 'user', 'onPremisesUserPrincipalName', '$.data.onPremisesUserPrincipalName', NULL, 'STRING', NULL, NULL, false, 92),
('Microsoft', 'users', 'user', 'onPremisesDistinguishedName', '$.data.onPremisesDistinguishedName', NULL, 'STRING', NULL, NULL, false, 93),
('Microsoft', 'users', 'user', 'onPremisesDomainName', '$.data.onPremisesDomainName', NULL, 'STRING', NULL, NULL, false, 94),
('Microsoft', 'users', 'user', 'onPremisesSyncEnabled', '$.data.onPremisesSyncEnabled', NULL, 'BOOLEAN', NULL, NULL, false, 95),
('Microsoft', 'users', 'user', 'onPremisesLastSyncDateTime', '$.data.onPremisesLastSyncDateTime', NULL, 'TIMESTAMP', 'parse_iso_date', NULL, false, 96),

-- Age and consent fields
('Microsoft', 'users', 'user', 'ageGroup', '$.data.ageGroup', NULL, 'STRING', NULL, NULL, false, 100),
('Microsoft', 'users', 'user', 'consentProvidedForMinor', '$.data.consentProvidedForMinor', NULL, 'STRING', NULL, NULL, false, 101),
('Microsoft', 'users', 'user', 'legalAgeGroupClassification', '$.data.legalAgeGroupClassification', NULL, 'STRING', NULL, NULL, false, 102),

-- Additional account fields
('Microsoft', 'users', 'user', 'passwordPolicies', '$.data.passwordPolicies', NULL, 'STRING', NULL, NULL, false, 110),
('Microsoft', 'users', 'user', 'preferredLanguage', '$.data.preferredLanguage', NULL, 'STRING', NULL, NULL, false, 111),
('Microsoft', 'users', 'user', 'isResourceAccount', '$.data.isResourceAccount', NULL, 'BOOLEAN', NULL, NULL, false, 112),
('Microsoft', 'users', 'user', 'showInAddressList', '$.data.showInAddressList', NULL, 'BOOLEAN', NULL, NULL, false, 113),

-- License and plan fields (JSON objects/arrays)
('Microsoft', 'users', 'user', 'assignedLicenses', '$.data.assignedLicenses', NULL, 'JSON', NULL, NULL, false, 120),
('Microsoft', 'users', 'user', 'assignedPlans', '$.data.assignedPlans', NULL, 'JSON', NULL, NULL, false, 121),
('Microsoft', 'users', 'user', 'provisionedPlans', '$.data.provisionedPlans', NULL, 'JSON', NULL, NULL, false, 122),
('Microsoft', 'users', 'user', 'licenseAssignmentStates', '$.data.licenseAssignmentStates', NULL, 'JSON', NULL, NULL, false, 123),

-- MFA and authentication fields
('Microsoft', 'users', 'user', 'authenticationMethods', '$.data.authenticationMethods', NULL, 'JSON', NULL, NULL, false, 130),
('Microsoft', 'users', 'user', 'mfaDetails', '$.data.mfaDetails', '["$.data.registrationDetails"]', 'JSON', NULL, NULL, false, 131),
('Microsoft', 'users', 'user', 'registrationDetails', '$.data.registrationDetails', NULL, 'JSON', NULL, NULL, false, 132),

-- Extra info (catch-all for additional data)
('Microsoft', 'users', 'user', 'extraInfo', '$.data', NULL, 'JSON', 'build_extra_info', NULL, false, 140)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- ACCOUNT ENTITY - Add missing fields
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, default_value, required, priority) VALUES
('Microsoft', 'users', 'account', 'tenantId', '$.tenant', 'UUID', 'uuid_from_string', '{"field": "tenant"}', NULL, true, 2),
('Microsoft', 'users', 'account', 'microsoftId', '$.data.id', 'STRING', NULL, NULL, NULL, false, 7)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- GROUP ENTITY - Add missing fields
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'group', 'tenantId', '$.tenant', 'UUID', 'uuid_from_string', '{"field": "tenant"}', true, 3),
('Microsoft', 'users', 'group', 'extraInfo', '$.data.groups[*]', 'JSON', 'build_group_extra_info', NULL, false, 60)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- ROLE ENTITY - Add missing fields
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'role', 'tenantId', '$.tenant', 'UUID', 'uuid_from_string', '{"field": "tenant"}', true, 3),
('Microsoft', 'users', 'role', 'extraInfo', '$.data.roles[*]', 'JSON', 'build_role_extra_info', NULL, false, 40)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- DEPARTMENT ENTITY - Add missing fields
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'department', 'tenantId', '$.tenant', 'UUID', 'uuid_from_string', '{"field": "tenant"}', true, 3)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

-- ============================================================================
-- OFFICE LOCATION ENTITY - Add missing fields
-- ============================================================================
INSERT INTO field_mappings (vendor, entity_type, target_entity, target_field, source_path, data_type, transform, transform_args, required, priority) VALUES
('Microsoft', 'users', 'office_location', 'tenantId', '$.tenant', 'UUID', 'uuid_from_string', '{"field": "tenant"}', true, 3)
ON CONFLICT (vendor, entity_type, target_entity, target_field) DO NOTHING;

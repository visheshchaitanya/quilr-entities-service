-- Seed canonical_fields table with entity field definitions

-- UserEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('user', 'userId', 'UUID', true, 'Primary key - deterministic UUID'),
('user', 'tenantId', 'UUID', true, 'Foreign key to tenant'),
('user', 'instanceId', 'UUID', true, 'Foreign key to instance'),
('user', 'id', 'STRING', true, 'Vendor-specific user ID'),
('user', 'displayName', 'STRING', false, 'User display name'),
('user', 'givenName', 'STRING', false, 'First name'),
('user', 'surname', 'STRING', false, 'Last name'),
('user', 'mail', 'STRING', false, 'Primary email address (lowercase)'),
('user', 'userPrincipalName', 'STRING', false, 'User principal name (lowercase)'),
('user', 'mobilePhone', 'STRING', false, 'Mobile phone number'),
('user', 'jobTitle', 'STRING', false, 'Job title'),
('user', 'employeeType', 'STRING', false, 'Employee type'),
('user', 'employeeHireDate', 'DATE', false, 'Hire date'),
('user', 'terminationDate', 'DATE', false, 'Termination date'),
('user', 'accountEnabled', 'BOOLEAN', false, 'Account enabled status'),
('user', 'userType', 'STRING', false, 'User type (Member/Guest)'),
('user', 'extensionDeploymentStatus', 'STRING', false, 'Extension deployment status'),
('user', 'userCreationTime', 'TIMESTAMP', false, 'User creation timestamp'),
('user', 'userLastLoginTime', 'TIMESTAMP', false, 'Last login timestamp'),
('user', 'extraInfo', 'JSONB', false, 'Additional user information')
ON CONFLICT (entity_type, field_name) DO NOTHING;

-- TenantEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('tenant', 'tenantId', 'UUID', true, 'Primary key'),
('tenant', 'id', 'STRING', true, 'Tenant ID string'),
('tenant', 'subscriberId', 'UUID', false, 'Subscriber ID')
ON CONFLICT (entity_type, field_name) DO NOTHING;

-- InstanceEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('instance', 'instanceId', 'UUID', true, 'Primary key'),
('instance', 'tenantId', 'UUID', true, 'Foreign key to tenant'),
('instance', 'appId', 'STRING', false, 'Application domain')
ON CONFLICT (entity_type, field_name) DO NOTHING;

-- ApplicationEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('application', 'id_', 'STRING', true, 'Primary key - application ID'),
('application', 'domain', 'STRING', false, 'Application domain'),
('application', 'newApp', 'BOOLEAN', false, 'Is new application'),
('application', 'globalSyncAllowed', 'BOOLEAN', false, 'Global sync allowed')
ON CONFLICT (entity_type, field_name) DO NOTHING;

-- AccountEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('account', 'id', 'STRING', true, 'Primary key - composite account ID'),
('account', 'tenantId', 'UUID', true, 'Foreign key to tenant'),
('account', 'email', 'STRING', false, 'Account email'),
('account', 'appName', 'STRING', false, 'Application name'),
('account', 'appId', 'STRING', false, 'Application ID'),
('account', 'microsoftId', 'STRING', false, 'Microsoft ID'),
('account', 'creationTime', 'TIMESTAMP', false, 'Account creation time')
ON CONFLICT (entity_type, field_name) DO NOTHING;

-- GroupEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('group', 'groupId', 'UUID', true, 'Primary key - deterministic UUID'),
('group', 'tenantId', 'UUID', true, 'Foreign key to tenant'),
('group', 'id', 'STRING', true, 'Vendor-specific group ID'),
('group', 'displayName', 'STRING', false, 'Group display name'),
('group', 'mail', 'STRING', false, 'Group email'),
('group', 'mailEnabled', 'BOOLEAN', false, 'Mail enabled status'),
('group', 'securityEnabled', 'BOOLEAN', false, 'Security enabled status'),
('group', 'groupTypes', 'ARRAY', false, 'Group types array'),
('group', 'createdDateTime', 'TIMESTAMP', false, 'Creation timestamp'),
('group', 'description', 'STRING', false, 'Group description'),
('group', 'visibility', 'STRING', false, 'Visibility setting'),
('group', 'classification', 'STRING', false, 'Classification'),
('group', 'mailNickname', 'STRING', false, 'Mail nickname'),
('group', 'membershipRule', 'STRING', false, 'Membership rule'),
('group', 'membershipRuleProcessingState', 'STRING', false, 'Membership rule processing state'),
('group', 'preferredDataLocation', 'STRING', false, 'Preferred data location'),
('group', 'preferredLanguage', 'STRING', false, 'Preferred language'),
('group', 'renewedDateTime', 'TIMESTAMP', false, 'Renewed timestamp'),
('group', 'theme', 'STRING', false, 'Theme'),
('group', 'uniqueName', 'STRING', false, 'Unique name'),
('group', 'isAssignableToRole', 'BOOLEAN', false, 'Assignable to role'),
('group', 'extraInfo', 'JSONB', false, 'Additional group information')
ON CONFLICT (entity_type, field_name) DO NOTHING;

-- RoleEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('role', 'roleId', 'UUID', true, 'Primary key - deterministic UUID'),
('role', 'tenantId', 'UUID', true, 'Foreign key to tenant'),
('role', 'id', 'STRING', true, 'Vendor-specific role ID'),
('role', 'displayName', 'STRING', false, 'Role display name'),
('role', 'description', 'STRING', false, 'Role description'),
('role', 'isBuiltIn', 'BOOLEAN', false, 'Is built-in role'),
('role', 'isEnabled', 'BOOLEAN', false, 'Is enabled'),
('role', 'isPrivileged', 'BOOLEAN', false, 'Is privileged role'),
('role', 'roleTemplateId', 'STRING', false, 'Role template ID'),
('role', 'assignmentType', 'STRING', false, 'Assignment type'),
('role', 'extraInfo', 'JSONB', false, 'Additional role information')
ON CONFLICT (entity_type, field_name) DO NOTHING;

-- DepartmentEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('department', 'departmentId', 'UUID', true, 'Primary key - deterministic UUID'),
('department', 'tenantId', 'UUID', true, 'Foreign key to tenant'),
('department', 'id', 'STRING', true, 'Department ID (name)'),
('department', 'name', 'STRING', false, 'Department name')
ON CONFLICT (entity_type, field_name) DO NOTHING;

-- OfficeLocationEntity fields
INSERT INTO canonical_fields (entity_type, field_name, data_type, required, description) VALUES
('office_location', 'officeLocationId', 'UUID', true, 'Primary key - deterministic UUID'),
('office_location', 'tenantId', 'UUID', true, 'Foreign key to tenant'),
('office_location', 'id', 'STRING', true, 'Office location ID (name)'),
('office_location', 'name', 'STRING', false, 'Office location name')
ON CONFLICT (entity_type, field_name) DO NOTHING;

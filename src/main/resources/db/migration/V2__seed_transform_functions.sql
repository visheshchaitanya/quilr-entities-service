-- Seed transform_functions table with all built-in transforms
INSERT INTO transform_functions (name, description, function_type, implementation_class) VALUES
('lowercase', 'Convert string to lowercase', 'STRING', 'com.quilr.mapping.transform.LowercaseTransform'),
('uppercase', 'Convert string to uppercase', 'STRING', 'com.quilr.mapping.transform.UppercaseTransform'),
('trim', 'Remove leading and trailing whitespace', 'STRING', 'com.quilr.mapping.transform.TrimTransform'),
('coalesce', 'Return first non-null value from multiple fields', 'UTILITY', 'com.quilr.mapping.transform.CoalesceTransform'),
('concat', 'Concatenate multiple values with separator', 'STRING', 'com.quilr.mapping.transform.ConcatTransform'),
('split', 'Split string and take element at index', 'STRING', 'com.quilr.mapping.transform.SplitTransform'),
('parse_iso_date', 'Parse ISO8601 date string to Instant', 'DATE', 'com.quilr.mapping.transform.ParseIsoDateTransform'),
('boolean_from_string', 'Parse boolean from string', 'CONVERSION', 'com.quilr.mapping.transform.BooleanFromStringTransform'),
('extract_domain', 'Extract domain from email address', 'STRING', 'com.quilr.mapping.transform.ExtractDomainTransform'),
('array_first', 'Get first element from array', 'ARRAY', 'com.quilr.mapping.transform.ArrayFirstTransform'),
('array_join', 'Join array elements to string', 'ARRAY', 'com.quilr.mapping.transform.ArrayJoinTransform'),
('default_if_null', 'Return default value if input is null', 'UTILITY', 'com.quilr.mapping.transform.DefaultIfNullTransform'),
('conditional', 'If-then-else conditional logic', 'LOGIC', 'com.quilr.mapping.transform.ConditionalTransform'),
('nested_lookup', 'Navigate nested JSON structure', 'JSON', 'com.quilr.mapping.transform.NestedLookupTransform'),
('uuid_from_bytes', 'Generate deterministic UUID from concatenated fields', 'UUID', 'com.quilr.mapping.transform.UuidFromBytesTransform'),
('uuid_from_string', 'Parse UUID from string', 'UUID', 'com.quilr.mapping.transform.UuidFromStringTransform'),
('extract_secondary_mail', 'Extract domain-matching secondary email from otherMails', 'CUSTOM', 'com.quilr.mapping.transform.ExtractSecondaryMailTransform'),
('extract_email_secondary', 'Extract non-domain-matching email from otherMails', 'CUSTOM', 'com.quilr.mapping.transform.ExtractEmailSecondaryTransform'),
('preferred_business_phone', 'Extract first non-empty business phone', 'CUSTOM', 'com.quilr.mapping.transform.PreferredBusinessPhoneTransform'),
('build_account_id', 'Build account ID from email and appId', 'CUSTOM', 'com.quilr.mapping.transform.BuildAccountIdTransform'),
('extension_deployment_status', 'Determine extension deployment status', 'CUSTOM', 'com.quilr.mapping.transform.ExtensionDeploymentStatusTransform')
ON CONFLICT (name) DO NOTHING;

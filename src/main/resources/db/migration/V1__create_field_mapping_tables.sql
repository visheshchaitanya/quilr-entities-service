-- Create field_mappings table
CREATE TABLE IF NOT EXISTS field_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    target_entity VARCHAR(50) NOT NULL,
    target_field VARCHAR(100) NOT NULL,
    source_path VARCHAR(500),
    fallback_paths JSONB,
    data_type VARCHAR(50) NOT NULL,
    transform VARCHAR(100),
    transform_args JSONB,
    condition VARCHAR(500),
    default_value TEXT,
    required BOOLEAN DEFAULT FALSE,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_field_mapping UNIQUE (vendor, entity_type, target_entity, target_field)
);

CREATE INDEX idx_field_mappings_vendor_entity ON field_mappings(vendor, entity_type);
CREATE INDEX idx_field_mappings_target ON field_mappings(target_entity);

-- Create transform_functions table
CREATE TABLE IF NOT EXISTS transform_functions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    function_type VARCHAR(50) NOT NULL,
    implementation_class VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create canonical_fields table
CREATE TABLE IF NOT EXISTS canonical_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    required BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_canonical_field UNIQUE (entity_type, field_name)
);

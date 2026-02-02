package com.quilr.repository;

import com.quilr.dto.entities.TenantEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tenant table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class TenantRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public TenantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert tenant entity using INSERT ON CONFLICT
     * @param entity Tenant entity to upsert
     * @return UUID of the upserted tenant
     */
    public UUID upsert(TenantEntity entity) {
        if (entity.getTenantId() == null) {
            throw new IllegalArgumentException("tenant_id is required");
        }
        
        String sql = """
            INSERT INTO tenant (
                tenant_id, id, name, subscriber_id, creation_time,
                enable_persona_via_background_tabs, enable_persona_via_forced_login,
                extension_enabled, created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (tenant_id) DO UPDATE SET
                name = EXCLUDED.name,
                subscriber_id = EXCLUDED.subscriber_id,
                creation_time = EXCLUDED.creation_time,
                enable_persona_via_background_tabs = EXCLUDED.enable_persona_via_background_tabs,
                enable_persona_via_forced_login = EXCLUDED.enable_persona_via_forced_login,
                extension_enabled = EXCLUDED.extension_enabled,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            RETURNING tenant_id
            """;
        
        return jdbcTemplate.queryForObject(sql, UUID.class,
            entity.getTenantId(),
            entity.getId(),
            entity.getName(),
            entity.getSubscriberId(),
            entity.getCreationTime() != null ? java.sql.Timestamp.from(entity.getCreationTime()) : null,
            entity.getEnablePersonaViaBackgroundTabs(),
            entity.getEnablePersonaViaForcedLogin(),
            entity.getExtensionEnabled(),
            java.sql.Timestamp.from(entity.getCreatedAt()),
            java.sql.Timestamp.from(entity.getUpdatedAt()),
            entity.getIsActive(),
            toJsonString(entity.getExtraInfo())
        );
    }
    
    /**
     * Check if tenant exists by id
     */
    public boolean existsById(String id) {
        String sql = "SELECT COUNT(*) FROM tenant WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
    
    /**
     * Find tenant by id
     */
    public Optional<UUID> findTenantIdById(String id) {
        String sql = "SELECT tenant_id FROM tenant WHERE id = ? AND is_active = true";
        try {
            UUID tenantId = jdbcTemplate.queryForObject(sql, UUID.class, id);
            return Optional.ofNullable(tenantId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

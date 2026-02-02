package com.quilr.repository;

import com.quilr.dto.entities.InstanceEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for instance table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class InstanceRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public InstanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert instance entity using INSERT ON CONFLICT
     * @param entity Instance entity to upsert
     * @return UUID of the upserted instance
     */
    public UUID upsert(InstanceEntity entity) {
        if (entity.getInstanceId() == null) {
            throw new IllegalArgumentException("instance_id is required");
        }
        if (entity.getTenantId() == null) {
            throw new IllegalArgumentException("tenant_id is required");
        }
        
        String sql = """
            INSERT INTO instance (
                instance_id, tenant_id, app_id, creation_time,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (instance_id) DO UPDATE SET
                creation_time = EXCLUDED.creation_time,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            RETURNING instance_id
            """;
        
        return jdbcTemplate.queryForObject(sql, UUID.class,
            entity.getInstanceId(),
            entity.getTenantId(),
            entity.getAppId(),
            entity.getCreationTime() != null ? java.sql.Timestamp.from(entity.getCreationTime()) : null,
            java.sql.Timestamp.from(entity.getCreatedAt()),
            java.sql.Timestamp.from(entity.getUpdatedAt()),
            entity.getIsActive(),
            toJsonString(entity.getExtraInfo())
        );
    }
    
    /**
     * Check if instance exists by tenant_id and app_id
     */
    public boolean existsByTenantIdAndAppId(UUID tenantId, String appId) {
        String sql = "SELECT COUNT(*) FROM instance WHERE tenant_id = ? AND app_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, appId);
        return count != null && count > 0;
    }
    
    /**
     * Find instance by tenant_id and app_id
     */
    public Optional<UUID> findInstanceIdByTenantIdAndAppId(UUID tenantId, String appId) {
        String sql = "SELECT instance_id FROM instance WHERE tenant_id = ? AND app_id = ? AND is_active = true";
        try {
            UUID instanceId = jdbcTemplate.queryForObject(sql, UUID.class, tenantId, appId);
            return Optional.ofNullable(instanceId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

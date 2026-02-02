package com.quilr.repository;

import com.quilr.dto.entities.OfficeLocationEntity;
import com.quilr.dto.entities.UserOfficeLocationLink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for office_location table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class OfficeLocationRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public OfficeLocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert office location entity using INSERT ON CONFLICT
     * @param entity Office location entity to upsert
     * @return UUID of the upserted office location
     */
    public UUID upsert(OfficeLocationEntity entity) {
        entity.ensureOfficeLocationId();
        
        String sql = """
            INSERT INTO office_location (
                office_location_id, tenant_id, id, name,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (tenant_id, id) DO UPDATE SET
                office_location_id = office_location.office_location_id,
                name = EXCLUDED.name,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            RETURNING office_location_id
            """;
        
        return jdbcTemplate.queryForObject(sql, UUID.class,
            entity.getOfficeLocationId(),
            entity.getTenantId(),
            entity.getId(),
            entity.getName(),
            java.sql.Timestamp.from(entity.getCreatedAt()),
            java.sql.Timestamp.from(entity.getUpdatedAt()),
            entity.getIsActive(),
            toJsonString(entity.getExtraInfo())
        );
    }
    
    /**
     * Find office location by tenant_id and id
     */
    public Optional<UUID> findOfficeLocationIdByTenantIdAndId(UUID tenantId, String id) {
        String sql = "SELECT office_location_id FROM office_location WHERE tenant_id = ? AND id = ? AND is_active = true";
        try {
            UUID officeLocationId = jdbcTemplate.queryForObject(sql, UUID.class, tenantId, id);
            return Optional.ofNullable(officeLocationId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Upsert user-office location link
     */
    public void upsertUserOfficeLocationLink(UserOfficeLocationLink link) {
        String sql = """
            INSERT INTO user_office_location (
                user_id, office_location_id, is_primary,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (user_id, office_location_id) DO UPDATE SET
                is_primary = EXCLUDED.is_primary,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            """;
        
        jdbcTemplate.update(sql,
            link.getUserId(),
            link.getOfficeLocationId(),
            link.getIsPrimary(),
            java.sql.Timestamp.from(link.getCreatedAt()),
            java.sql.Timestamp.from(link.getUpdatedAt()),
            link.getIsActive(),
            toJsonString(link.getExtraInfo())
        );
    }
}

package com.quilr.repository;

import com.quilr.dto.entities.DepartmentEntity;
import com.quilr.dto.entities.UserDepartmentLink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for department table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class DepartmentRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public DepartmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert department entity using INSERT ON CONFLICT
     * @param entity Department entity to upsert
     * @return UUID of the upserted department
     */
    public UUID upsert(DepartmentEntity entity) {
        entity.ensureDepartmentId();
        
        String sql = """
            INSERT INTO department (
                department_id, tenant_id, id, name,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (tenant_id, id) DO UPDATE SET
                department_id = department.department_id,
                name = EXCLUDED.name,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            RETURNING department_id
            """;
        
        return jdbcTemplate.queryForObject(sql, UUID.class,
            entity.getDepartmentId(),
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
     * Find department by tenant_id and id
     */
    public Optional<UUID> findDepartmentIdByTenantIdAndId(UUID tenantId, String id) {
        String sql = "SELECT department_id FROM department WHERE tenant_id = ? AND id = ? AND is_active = true";
        try {
            UUID departmentId = jdbcTemplate.queryForObject(sql, UUID.class, tenantId, id);
            return Optional.ofNullable(departmentId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Upsert user-department link
     */
    public void upsertUserDepartmentLink(UserDepartmentLink link) {
        String sql = """
            INSERT INTO user_department (
                user_id, tenant_id, department_id, is_primary,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (user_id, department_id) DO UPDATE SET
                is_primary = EXCLUDED.is_primary,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            """;
        
        jdbcTemplate.update(sql,
            link.getUserId(),
            link.getTenantId(),
            link.getDepartmentId(),
            link.getIsPrimary(),
            java.sql.Timestamp.from(link.getCreatedAt()),
            java.sql.Timestamp.from(link.getUpdatedAt()),
            link.getIsActive(),
            toJsonString(link.getExtraInfo())
        );
    }
}

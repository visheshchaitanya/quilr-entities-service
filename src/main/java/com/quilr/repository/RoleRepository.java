package com.quilr.repository;

import com.quilr.dto.entities.RoleEntity;
import com.quilr.dto.entities.UserRoleLink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for roles table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class RoleRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public RoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert role entity using INSERT ON CONFLICT
     * @param entity Role entity to upsert
     * @return UUID of the upserted role
     */
    public UUID upsert(RoleEntity entity) {
        entity.ensureRoleId();
        
        String sql = """
            INSERT INTO roles (
                role_id, tenant_id, id, display_name, description,
                is_built_in, is_enabled, is_privileged, role_template_id, assignment_type,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (tenant_id, id) DO UPDATE SET
                role_id = roles.role_id,
                display_name = EXCLUDED.display_name,
                description = EXCLUDED.description,
                is_built_in = EXCLUDED.is_built_in,
                is_enabled = EXCLUDED.is_enabled,
                is_privileged = EXCLUDED.is_privileged,
                role_template_id = EXCLUDED.role_template_id,
                assignment_type = EXCLUDED.assignment_type,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            RETURNING role_id
            """;
        
        return jdbcTemplate.queryForObject(sql, UUID.class,
            entity.getRoleId(),
            entity.getTenantId(),
            entity.getId(),
            entity.getDisplayName(),
            entity.getDescription(),
            entity.getIsBuiltIn(),
            entity.getIsEnabled(),
            entity.getIsPrivileged(),
            entity.getRoleTemplateId(),
            entity.getAssignmentType(),
            java.sql.Timestamp.from(entity.getCreatedAt()),
            java.sql.Timestamp.from(entity.getUpdatedAt()),
            entity.getIsActive(),
            toJsonString(entity.getExtraInfo())
        );
    }
    
    /**
     * Find role by tenant_id and id
     */
    public Optional<UUID> findRoleIdByTenantIdAndId(UUID tenantId, String id) {
        String sql = "SELECT role_id FROM roles WHERE tenant_id = ? AND id = ? AND is_active = true";
        try {
            UUID roleId = jdbcTemplate.queryForObject(sql, UUID.class, tenantId, id);
            return Optional.ofNullable(roleId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Upsert user-role link
     */
    public void upsertUserRoleLink(UserRoleLink link) {
        String sql = """
            INSERT INTO user_roles (
                user_id, role_id, assignment_type,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (user_id, role_id) DO UPDATE SET
                assignment_type = EXCLUDED.assignment_type,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            """;
        
        jdbcTemplate.update(sql,
            link.getUserId(),
            link.getRoleId(),
            link.getAssignmentType(),
            java.sql.Timestamp.from(link.getCreatedAt()),
            java.sql.Timestamp.from(link.getUpdatedAt()),
            link.getIsActive(),
            toJsonString(link.getExtraInfo())
        );
    }
}

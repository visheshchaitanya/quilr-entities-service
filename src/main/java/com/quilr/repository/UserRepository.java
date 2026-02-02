package com.quilr.repository;

import com.quilr.dto.entities.UserEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class UserRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert user entity using INSERT ON CONFLICT
     * @param entity User entity to upsert
     * @return UUID of the upserted user
     */
    public UUID upsert(UserEntity entity) {
        entity.ensureUserId();
        
        String sql = """
            INSERT INTO "user" (
                user_id, tenant_id, instance_id, id, display_name, given_name, surname,
                mail, user_principal_name, mobile_phone, job_title, employee_type,
                employee_hire_date, termination_date, account_enabled, user_suspended,
                user_archived, user_type, user_is_admin, user_delegation_admin,
                user_ip_whitelisted, extension_enabled, extension_deployment_status,
                user_creation_time, user_last_login_time, profile_pic_url,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (tenant_id, id) DO UPDATE SET
                display_name = EXCLUDED.display_name,
                given_name = EXCLUDED.given_name,
                surname = EXCLUDED.surname,
                mail = EXCLUDED.mail,
                user_principal_name = EXCLUDED.user_principal_name,
                mobile_phone = EXCLUDED.mobile_phone,
                job_title = EXCLUDED.job_title,
                employee_type = EXCLUDED.employee_type,
                employee_hire_date = EXCLUDED.employee_hire_date,
                termination_date = EXCLUDED.termination_date,
                account_enabled = EXCLUDED.account_enabled,
                user_suspended = EXCLUDED.user_suspended,
                user_archived = EXCLUDED.user_archived,
                user_type = EXCLUDED.user_type,
                user_is_admin = EXCLUDED.user_is_admin,
                user_delegation_admin = EXCLUDED.user_delegation_admin,
                user_ip_whitelisted = EXCLUDED.user_ip_whitelisted,
                extension_enabled = EXCLUDED.extension_enabled,
                extension_deployment_status = EXCLUDED.extension_deployment_status,
                user_creation_time = EXCLUDED.user_creation_time,
                user_last_login_time = EXCLUDED.user_last_login_time,
                profile_pic_url = EXCLUDED.profile_pic_url,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            RETURNING user_id
            """;
        
        return jdbcTemplate.queryForObject(sql, UUID.class,
            entity.getUserId(),
            entity.getTenantId(),
            entity.getInstanceId(),
            entity.getId(),
            entity.getDisplayName(),
            entity.getGivenName(),
            entity.getSurname(),
            entity.getMail(),
            entity.getUserPrincipalName(),
            entity.getMobilePhone(),
            entity.getJobTitle(),
            entity.getEmployeeType(),
            entity.getEmployeeHireDate(),
            entity.getTerminationDate(),
            entity.getAccountEnabled(),
            entity.getUserSuspended(),
            entity.getUserArchived(),
            entity.getUserType(),
            entity.getUserIsAdmin(),
            entity.getUserDelegationAdmin(),
            entity.getUserIpWhitelisted(),
            entity.getExtensionEnabled(),
            entity.getExtensionDeploymentStatus(),
            entity.getUserCreationTime() != null ? java.sql.Timestamp.from(entity.getUserCreationTime()) : null,
            entity.getUserLastLoginTime() != null ? java.sql.Timestamp.from(entity.getUserLastLoginTime()) : null,
            entity.getProfilePicUrl(),
            java.sql.Timestamp.from(entity.getCreatedAt()),
            java.sql.Timestamp.from(entity.getUpdatedAt()),
            entity.getIsActive(),
            toJsonString(entity.getExtraInfo())
        );
    }
    
    /**
     * Find user by tenant_id and id
     */
    public Optional<UUID> findUserIdByTenantIdAndId(UUID tenantId, String id) {
        String sql = "SELECT user_id FROM \"user\" WHERE tenant_id = ? AND id = ? AND is_active = true";
        try {
            UUID userId = jdbcTemplate.queryForObject(sql, UUID.class, tenantId, id);
            return Optional.ofNullable(userId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

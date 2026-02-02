package com.quilr.repository;

import com.quilr.dto.entities.GroupEntity;
import com.quilr.dto.entities.UserGroupLink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for groups table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class GroupRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public GroupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert group entity using INSERT ON CONFLICT
     * @param entity Group entity to upsert
     * @return UUID of the upserted group
     */
    public UUID upsert(GroupEntity entity) {
        entity.ensureGroupId();
        
        String sql = """
            INSERT INTO groups (
                group_id, tenant_id, id, display_name, mail, mail_enabled, security_enabled,
                group_types, created_date_time, description, visibility, classification,
                mail_nickname, membership_rule, membership_rule_processing_state,
                preferred_data_location, preferred_language, renewed_date_time,
                theme, unique_name, is_assignable_to_role,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (tenant_id, id) DO UPDATE SET
                group_id = groups.group_id,
                display_name = EXCLUDED.display_name,
                mail = EXCLUDED.mail,
                mail_enabled = EXCLUDED.mail_enabled,
                security_enabled = EXCLUDED.security_enabled,
                group_types = EXCLUDED.group_types,
                created_date_time = EXCLUDED.created_date_time,
                description = EXCLUDED.description,
                visibility = EXCLUDED.visibility,
                classification = EXCLUDED.classification,
                mail_nickname = EXCLUDED.mail_nickname,
                membership_rule = EXCLUDED.membership_rule,
                membership_rule_processing_state = EXCLUDED.membership_rule_processing_state,
                preferred_data_location = EXCLUDED.preferred_data_location,
                preferred_language = EXCLUDED.preferred_language,
                renewed_date_time = EXCLUDED.renewed_date_time,
                theme = EXCLUDED.theme,
                unique_name = EXCLUDED.unique_name,
                is_assignable_to_role = EXCLUDED.is_assignable_to_role,
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            RETURNING group_id
            """;
        
        return jdbcTemplate.queryForObject(sql, UUID.class,
            entity.getGroupId(),
            entity.getTenantId(),
            entity.getId(),
            entity.getDisplayName(),
            entity.getMail(),
            entity.getMailEnabled(),
            entity.getSecurityEnabled(),
            entity.getGroupTypes() != null ? entity.getGroupTypes().toArray(new String[0]) : null,
            entity.getCreatedDateTime() != null ? java.sql.Timestamp.from(entity.getCreatedDateTime()) : null,
            entity.getDescription(),
            entity.getVisibility(),
            entity.getClassification(),
            entity.getMailNickname(),
            entity.getMembershipRule(),
            entity.getMembershipRuleProcessingState(),
            entity.getPreferredDataLocation(),
            entity.getPreferredLanguage(),
            entity.getRenewedDateTime() != null ? java.sql.Timestamp.from(entity.getRenewedDateTime()) : null,
            entity.getTheme(),
            entity.getUniqueName(),
            entity.getIsAssignableToRole(),
            java.sql.Timestamp.from(entity.getCreatedAt()),
            java.sql.Timestamp.from(entity.getUpdatedAt()),
            entity.getIsActive(),
            toJsonString(entity.getExtraInfo())
        );
    }
    
    /**
     * Find group by tenant_id and id
     */
    public Optional<UUID> findGroupIdByTenantIdAndId(UUID tenantId, String id) {
        String sql = "SELECT group_id FROM groups WHERE tenant_id = ? AND id = ? AND is_active = true";
        try {
            UUID groupId = jdbcTemplate.queryForObject(sql, UUID.class, tenantId, id);
            return Optional.ofNullable(groupId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Upsert user-group link
     */
    public void upsertUserGroupLink(UserGroupLink link) {
        String sql = """
            INSERT INTO user_groups (
                user_id, group_id,
                created_at, updated_at, is_active, extra_info
            ) VALUES (?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (user_id, group_id) DO UPDATE SET
                updated_at = CURRENT_TIMESTAMP,
                is_active = EXCLUDED.is_active,
                extra_info = EXCLUDED.extra_info
            """;
        
        jdbcTemplate.update(sql,
            link.getUserId(),
            link.getGroupId(),
            java.sql.Timestamp.from(link.getCreatedAt()),
            java.sql.Timestamp.from(link.getUpdatedAt()),
            link.getIsActive(),
            toJsonString(link.getExtraInfo())
        );
    }
}

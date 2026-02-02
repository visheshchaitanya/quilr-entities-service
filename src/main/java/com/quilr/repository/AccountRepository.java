package com.quilr.repository;

import com.quilr.dto.entities.AccountEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for account table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class AccountRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert account entity using INSERT ON CONFLICT on composite PK (id, tenant_id)
     * @param entity Account entity to upsert
     */
    public void upsert(AccountEntity entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (entity.getTenantId() == null) {
            throw new IllegalArgumentException("tenant_id is required");
        }
        
        String sql = """
            INSERT INTO account (
                id, tenant_id, email, app_name, app_id, microsoft_id, 
                creation_time, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id, tenant_id) DO UPDATE SET
                email = EXCLUDED.email,
                app_name = EXCLUDED.app_name,
                app_id = EXCLUDED.app_id,
                microsoft_id = EXCLUDED.microsoft_id,
                creation_time = EXCLUDED.creation_time,
                updated_at = CURRENT_TIMESTAMP
            """;
        
        jdbcTemplate.update(sql,
            entity.getId(),
            entity.getTenantId(),
            entity.getEmail(),
            entity.getAppName(),
            entity.getAppId(),
            entity.getMicrosoftId(),
            entity.getCreationTime() != null ? java.sql.Timestamp.from(entity.getCreationTime()) : null,
            java.sql.Timestamp.from(entity.getCreatedAt()),
            java.sql.Timestamp.from(entity.getUpdatedAt())
        );
    }
    
    /**
     * Check if account exists by id and tenant_id
     */
    public boolean existsByIdAndTenantId(String id, UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM account WHERE id = ? AND tenant_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id, tenantId);
        return count != null && count > 0;
    }
    
    /**
     * Find account by id and tenant_id
     */
    public Optional<AccountEntity> findByIdAndTenantId(String id, UUID tenantId) {
        String sql = """
            SELECT id, tenant_id, email, app_name, app_id, microsoft_id, 
                   creation_time, created_at, updated_at
            FROM account 
            WHERE id = ? AND tenant_id = ?
            """;
        try {
            return jdbcTemplate.query(sql, rs -> {
                if (rs.next()) {
                    return Optional.of(AccountEntity.builder()
                        .id(rs.getString("id"))
                        .tenantId((UUID) rs.getObject("tenant_id"))
                        .email(rs.getString("email"))
                        .appName(rs.getString("app_name"))
                        .appId(rs.getString("app_id"))
                        .microsoftId(rs.getString("microsoft_id"))
                        .creationTime(rs.getTimestamp("creation_time") != null ? 
                            rs.getTimestamp("creation_time").toInstant() : null)
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .updatedAt(rs.getTimestamp("updated_at").toInstant())
                        .build());
                }
                return Optional.empty();
            }, id, tenantId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

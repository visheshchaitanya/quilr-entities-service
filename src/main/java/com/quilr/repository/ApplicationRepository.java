package com.quilr.repository;

import com.quilr.dto.entities.ApplicationEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for application table operations.
 * Uses native SQL with INSERT ON CONFLICT for upsert operations.
 */
@Repository
public class ApplicationRepository extends BaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public ApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Upsert application entity using INSERT ON CONFLICT on id_ (primary key)
     * @param entity Application entity to upsert
     * @return id_ of the upserted application
     */
    public String upsert(ApplicationEntity entity) {
        if (entity.getId_() == null) {
            throw new IllegalArgumentException("id_ is required");
        }
        
        String sql = """
            INSERT INTO application (
                id_, domain, new_app, global_sync_allowed, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id_) DO UPDATE SET
                domain = EXCLUDED.domain,
                new_app = EXCLUDED.new_app,
                global_sync_allowed = EXCLUDED.global_sync_allowed,
                updated_at = CURRENT_TIMESTAMP
            RETURNING id_
            """;
        
        return jdbcTemplate.queryForObject(sql, String.class,
            entity.getId_(),
            entity.getDomain(),
            entity.getNewApp(),
            entity.getGlobalSyncAllowed(),
            java.sql.Timestamp.from(entity.getCreatedAt()),
            java.sql.Timestamp.from(entity.getUpdatedAt())
        );
    }
    
    /**
     * Check if application exists by id_
     */
    public boolean existsById(String id) {
        String sql = "SELECT COUNT(*) FROM application WHERE id_ = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
    
    /**
     * Find application by id_
     */
    public Optional<String> findById(String id) {
        String sql = "SELECT id_ FROM application WHERE id_ = ?";
        try {
            String appId = jdbcTemplate.queryForObject(sql, String.class, id);
            return Optional.ofNullable(appId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

package com.quilr.repository;

import com.quilr.model.mapping.TransformFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for transform_functions table
 */
@Repository
public interface TransformFunctionRepository extends JpaRepository<TransformFunction, UUID> {
    
    /**
     * Find transform function by name
     */
    Optional<TransformFunction> findByName(String name);
}

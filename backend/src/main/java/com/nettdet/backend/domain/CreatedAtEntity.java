package com.nettdet.backend.domain;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Base class for entities that require a persistent record of their creation time.
 * 
 * <p>This {@link MappedSuperclass} uses Hibernate's {@link CreationTimestamp} to 
 * automatically populate the {@code created_at} column when the entity is first 
 * persisted. The value is immutable once set.
 */
@MappedSuperclass
public abstract class CreatedAtEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

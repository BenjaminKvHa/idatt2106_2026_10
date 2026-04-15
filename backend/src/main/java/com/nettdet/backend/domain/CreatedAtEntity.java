package com.nettdet.backend.domain;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

@MappedSuperclass
public abstract class CreatedAtEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

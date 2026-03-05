package com.orbit.portfolio.model;

import com.orbit.portfolio.model.audit.*;
import jakarta.persistence.*;
import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditListener.class)
public abstract class BaseEntity implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    protected Instant createdAt;
    protected Instant updatedAt;

    @Column(nullable = false)
    protected boolean isDeleted = false;

    protected BaseEntity() {}

    public Long getId() { return id; }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return isDeleted; }
    public void softDelete() { this.isDeleted = true; }
}

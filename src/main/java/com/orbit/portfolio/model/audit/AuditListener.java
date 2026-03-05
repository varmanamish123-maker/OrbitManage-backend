package com.orbit.portfolio.model.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

public class AuditListener {

    @PrePersist
    public void onCreate(Object entity) {
        if (entity instanceof Auditable a) {
            Instant now = Instant.now();
            a.setCreatedAt(now);
            a.setUpdatedAt(now);
        }
    }

    @PreUpdate
    public void onUpdate(Object entity) {
        if (entity instanceof Auditable a) {
            a.setUpdatedAt(Instant.now());
        }
    }
}

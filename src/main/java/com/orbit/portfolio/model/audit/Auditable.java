package com.orbit.portfolio.model.audit;

import java.time.Instant;

public interface Auditable {
    void setCreatedAt(Instant createdAt);
    void setUpdatedAt(Instant updatedAt);
}

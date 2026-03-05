package com.orbit.portfolio.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "close_price_history")
public class ClosePriceHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Asset asset;

    private BigDecimal closePrice;
    private Instant closePriceTimestamp;

    protected ClosePriceHistory() {}

    public ClosePriceHistory(Asset asset, BigDecimal closePrice, Instant closePriceTimestamp) {
        this.asset = asset;
        this.closePrice = closePrice;
        this.closePriceTimestamp = closePriceTimestamp;
    }

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }

    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }

    public Instant getClosePriceTimestamp() { return closePriceTimestamp; }
    public void setClosePriceTimestamp(Instant closePriceTimestamp) { this.closePriceTimestamp = closePriceTimestamp; }
}
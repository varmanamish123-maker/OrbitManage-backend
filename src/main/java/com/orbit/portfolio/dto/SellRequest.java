package com.orbit.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class SellRequest {
    private Long portfolioId;
    private Long holdingId;
    private BigDecimal quantity;
    private BigDecimal price;
    private Instant sellTimestamp;

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public Long getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Long holdingId) {
        this.holdingId = holdingId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Instant getSellTimestamp() {
        return sellTimestamp;
    }

    public void setSellTimestamp(Instant sellTimestamp) {
        this.sellTimestamp = sellTimestamp;
    }
}

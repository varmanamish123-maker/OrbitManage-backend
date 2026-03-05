package com.orbit.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbit.portfolio.model.Asset;

public class BuyRequest {
    private Asset asset;
    private Long portfolioId;
    private BigDecimal price; //optional
    private BigDecimal quantity;
    private Instant buyTimestamp;//optional

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }

    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public Instant getBuyTimestamp() { return buyTimestamp; }
    public void setBuyTimestamp(Instant buyTimestamp) { this.buyTimestamp = buyTimestamp; }
}

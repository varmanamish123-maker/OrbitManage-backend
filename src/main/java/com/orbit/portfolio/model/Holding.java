package com.orbit.portfolio.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "holdings")
public class Holding extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    private Asset asset;

    private BigDecimal quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal avgSellPrice;
    private Instant buyTimestamp;
    private Instant sellTimestamp;

    protected Holding() {}

    public Holding(
            Portfolio portfolio,
            Asset asset,
            BigDecimal quantity,
            BigDecimal avgBuyPrice,
            BigDecimal avgSellPrice,
            Instant buyTimestamp,
            Instant sellTimestamp
    ) {
        this.portfolio = portfolio;
        this.asset = asset;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
        this.avgSellPrice = avgSellPrice;
        this.buyTimestamp = buyTimestamp;
        this.sellTimestamp = sellTimestamp;
    }

    public Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getAvgBuyPrice() { return avgBuyPrice; }
    public void setAvgBuyPrice(BigDecimal avgBuyPrice) { this.avgBuyPrice = avgBuyPrice; }

    public BigDecimal getAvgSellPrice() { return avgSellPrice; }
    public void setAvgSellPrice(BigDecimal avgSellPrice) { this.avgSellPrice = avgSellPrice; }

    public Instant getBuyTimestamp() { return buyTimestamp; }
    public void setBuyTimestamp(Instant buyTimestamp) { this.buyTimestamp = buyTimestamp; }

    public Instant getSellTimestamp() { return sellTimestamp; }
    public void setSellTimestamp(Instant sellTimestamp) { this.sellTimestamp = sellTimestamp; }
}
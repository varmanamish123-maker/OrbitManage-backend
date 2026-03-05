package com.orbit.portfolio.model;

import com.orbit.portfolio.model.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Holding holding;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal quantity;
    private BigDecimal buyPrice;
    private Instant buyTimestamp;
    private BigDecimal sellPrice;
    private Instant sellTimestamp;
    private BigDecimal profitBooked;
    private Instant executedAt;

    protected Transaction() {} // JPA only

    public static Transaction buy(
            Holding holding,
            BigDecimal quantity,
            BigDecimal buyPrice,
            Instant buyTimestamp
    ) {
        Transaction tx = new Transaction();
        tx.holding = holding;
        tx.type = TransactionType.BUY;
        tx.quantity = quantity;
        tx.buyPrice = buyPrice;
        tx.buyTimestamp = buyTimestamp;
        tx.sellPrice = null;
        tx.sellTimestamp = null;
        tx.executedAt = buyTimestamp;
        tx.profitBooked = null;
        return tx;
    }

    public static Transaction sell(
            Holding holding,
            BigDecimal quantity,
            BigDecimal buyPrice,
            Instant buyTimestamp,
            BigDecimal sellPrice,
            Instant sellTimestamp,
            BigDecimal profitBooked
    ) {
        Transaction tx = new Transaction();
        tx.holding = holding;
        tx.type = TransactionType.SELL;
        tx.quantity = quantity;
        tx.buyPrice = buyPrice;
        tx.buyTimestamp = buyTimestamp;
        tx.sellPrice = sellPrice;
        tx.sellTimestamp = sellTimestamp;
        tx.executedAt = sellTimestamp;
        tx.profitBooked = profitBooked;
        return tx;
    }

    public static Transaction edit(
            Holding holding,
            BigDecimal quantity,
            BigDecimal buyPrice
    ) {
        Transaction tx = new Transaction();
        tx.holding = holding;
        tx.type = TransactionType.EDIT;
        tx.quantity = quantity;
        tx.buyPrice = buyPrice;
        tx.buyTimestamp = null;
        tx.sellPrice = null;
        tx.sellTimestamp = null;
        tx.executedAt = Instant.now();
        tx.profitBooked = null;
        return tx;
    }

    public Holding getHolding() { return holding; }
    public void setHolding(Holding holding) { this.holding = holding; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getBuyPrice() { return buyPrice; }
    public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }

    public Instant getBuyTimestamp() { return buyTimestamp; }
    public void setBuyTimestamp(Instant buyTimestamp) { this.buyTimestamp = buyTimestamp; }

    public BigDecimal getSellPrice() { return sellPrice; }
    public void setSellPrice(BigDecimal sellPrice) { this.sellPrice = sellPrice; }

    public Instant getSellTimestamp() { return sellTimestamp; }
    public void setSellTimestamp(Instant sellTimestamp) { this.sellTimestamp = sellTimestamp; }

    public BigDecimal getProfitBooked() { return profitBooked; }
    public void setProfitBooked(BigDecimal profitBooked) { this.profitBooked = profitBooked; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
}
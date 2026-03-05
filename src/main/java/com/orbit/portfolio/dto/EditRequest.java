package com.orbit.portfolio.dto;

import java.math.BigDecimal;

public class EditRequest {
    private Long portfolioId;
    private Long holdingId;
    private BigDecimal newQuantity;
    private BigDecimal newAverageBuyPrice;

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

    public BigDecimal getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(BigDecimal newQuantity) {
        this.newQuantity = newQuantity;
    }

    public BigDecimal getNewAverageBuyPrice() {
        return newAverageBuyPrice;
    }

    public void setNewAverageBuyPrice(BigDecimal newAverageBuyPrice) {
        this.newAverageBuyPrice = newAverageBuyPrice;
    }
}

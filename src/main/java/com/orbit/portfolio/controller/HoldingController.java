package com.orbit.portfolio.controller;

import com.orbit.portfolio.model.Holding;
import org.springframework.web.bind.annotation.*;

import com.orbit.portfolio.dto.BuyRequest;
import com.orbit.portfolio.dto.EditRequest;
import com.orbit.portfolio.dto.SellRequest;
import com.orbit.portfolio.service.HoldingService;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/portfolios")
public class HoldingController {

    private final HoldingService holdingService;
    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @PostMapping("/buy")
    public String buyHoldingEndpoint(@RequestBody BuyRequest buyRequest) {
        Long portfolioId = buyRequest.getPortfolioId() == null ? null : buyRequest.getPortfolioId().longValue();
        holdingService.buyHolding(
                portfolioId,
                buyRequest.getAsset(),
                buyRequest.getQuantity(),
                buyRequest.getPrice(),
                buyRequest.getBuyTimestamp()
        );
        return "Buy transaction executed for portfolio " + portfolioId;
    }

    @PostMapping("/sell")
    public String sellHoldingEndpoint(@RequestBody SellRequest sellRequest) {
        holdingService.sellHolding(
                sellRequest.getPortfolioId(),
                sellRequest.getHoldingId(),
                sellRequest.getQuantity(),
                sellRequest.getPrice(),
                sellRequest.getSellTimestamp()
        );
        return "Sell transaction executed for portfolio " + sellRequest.getPortfolioId();   
    }

    @PostMapping("/edit")
    public String editHoldingEndpoint(@RequestBody EditRequest editRequest) {
        holdingService.editHolding(
                editRequest.getPortfolioId(),
                editRequest.getHoldingId(),
                editRequest.getNewQuantity(),
                editRequest.getNewAverageBuyPrice()
        );
        return "Edit transaction executed for portfolio " + editRequest.getPortfolioId();
    }

    @PostMapping("/delete/{holdingId}")
    public String deleteHoldingEndpoint(@PathVariable Long holdingId) {
        holdingService.deleteHolding(
                holdingId
        );
        return "Delete transaction executed for holding " + holdingId;
    }

    @GetMapping("{portfolioId}/holdings")
    public List<Map<String, Object>> getCurrentHoldings(@PathVariable Long portfolioId, @RequestParam String assetType) {
        return holdingService.getCurrentHoldingsByAssetType(portfolioId, assetType);
    }

    @GetMapping("/{portfolioId}/holdings/history")
    public Iterable<Holding> getHistoricalHoldings(@PathVariable Long portfolioId, @RequestParam String assetType) {
        return holdingService.getHistoricalHoldingsByAssetType(portfolioId, assetType);
    }



}
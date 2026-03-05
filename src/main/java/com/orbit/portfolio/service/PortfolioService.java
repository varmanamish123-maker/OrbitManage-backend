package com.orbit.portfolio.service;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.orbit.portfolio.model.*;
import com.orbit.portfolio.model.enums.*;
import com.orbit.portfolio.repository.*;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    ////////////////////////
    //   PORTFOLIO STATS  //
    ////////////////////////
    // i wanna return these values from the portfolio stats api
    //     "totalRealisedPnl": 81818.4,
    // "totalUnrealisedPnlPercent": 595.3112,
    // "totalRealisedPnlPercent": 550.6816,
    // "totalUnrealisedPnl": 88449.31025756836,
    // "totalTodayUnrealisedPnl": 573.6705310058566,
    // "totalTodayUnrealisedPnlPercent": 3.8611

	private final AssetRepository assetRepository;
    private final ClosePriceHistoryRepository closePriceHistoryRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    
    public PortfolioService(AssetRepository assetRepository,
                            ClosePriceHistoryRepository closePriceHistoryRepository,
                            HoldingRepository holdingRepository,
                            PortfolioRepository portfolioRepository,
                            TransactionRepository transactionRepository,
                            UserRepository userRepository) {
        this.assetRepository = assetRepository;
        this.closePriceHistoryRepository = closePriceHistoryRepository;
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> getPortfolioStats(Long portfolioId) {
        
        // Implement logic, to get realsied PnL you will fetch all transactions for the portfolioid
        // realizedPnL = sum of profitBooked column  for all type == TranscationType.SELL transactions
        // unrealisedPnL = for each holding get the current price from FetchPrice class and then unrealisedPnL = (currentPrice - averageBuyPrice) * quantity where holding.delete is false and holding.avgSellPrice is null
        //like that send total realisedPnl, total unrealised pnl, total unrealised pnl percent, total realised pnl percent, total today unrealised pnl and total today realised pnl, today unrealised pnl in percent and today realised pnl in percent
        
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID must be provided");
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        BigDecimal totalRealisedPnl = BigDecimal.ZERO;
        BigDecimal totalUnrealisedPnl = BigDecimal.ZERO;
        BigDecimal totalTodayUnrealisedPnl = BigDecimal.ZERO;

        BigDecimal totalInvestedOpen = BigDecimal.ZERO;
        BigDecimal totalInvestedClosed = BigDecimal.ZERO;

        Iterable<Holding> openHoldings = holdingRepository
                .findByPortfolioAndSellTimestampIsNullAndIsDeletedFalse(portfolio);

        for (Holding holding : (List<Holding>) openHoldings) {
            if (holding.getAsset() == null || holding.getQuantity() == null || holding.getAvgBuyPrice() == null) {
                continue;
            }

            BigDecimal quantity = holding.getQuantity();
            BigDecimal avgBuyPrice = holding.getAvgBuyPrice();
            BigDecimal currentPrice = getCurrentPrice(holding.getAsset());
            BigDecimal lastClose = getLastClosePrice(holding.getAsset());

            BigDecimal unrealised = currentPrice.subtract(avgBuyPrice).multiply(quantity);
            totalUnrealisedPnl = totalUnrealisedPnl.add(unrealised);

            BigDecimal todayUnrealised = currentPrice.subtract(lastClose).multiply(quantity);
            totalTodayUnrealisedPnl = totalTodayUnrealisedPnl.add(todayUnrealised);

            totalInvestedOpen = totalInvestedOpen.add(avgBuyPrice.multiply(quantity));
        }

        Iterable<Holding> closedHoldings = holdingRepository
                .findByPortfolioAndSellTimestampIsNotNullAndIsDeletedFalse(portfolio);

        for (Holding holding : (List<Holding>) closedHoldings) {
            if (holding.getQuantity() != null && holding.getAvgBuyPrice() != null) {
                totalInvestedClosed = totalInvestedClosed.add(
                        holding.getAvgBuyPrice().multiply(holding.getQuantity())
                );
            }

            List<Transaction> transactions = transactionRepository
                    .findByHoldingIdAndIsDeletedFalseOrderByExecutedAtAsc(holding.getId());

            for (Transaction tx : transactions) {
                if (tx.getType() == TransactionType.SELL && tx.getProfitBooked() != null) {
                    totalRealisedPnl = totalRealisedPnl.add(tx.getProfitBooked());
                }
            }
        }

        BigDecimal totalUnrealisedPnlPercent = percent(totalUnrealisedPnl, totalInvestedOpen);
        BigDecimal totalRealisedPnlPercent = percent(totalRealisedPnl, totalInvestedClosed);
        BigDecimal totalTodayUnrealisedPnlPercent = percent(totalTodayUnrealisedPnl, totalInvestedOpen);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRealisedPnl", totalRealisedPnl);
        stats.put("totalUnrealisedPnlPercent", totalUnrealisedPnlPercent);
        stats.put("totalRealisedPnlPercent", totalRealisedPnlPercent);
        stats.put("totalUnrealisedPnl", totalUnrealisedPnl);
        stats.put("totalTodayUnrealisedPnl", totalTodayUnrealisedPnl);
        stats.put("totalTodayUnrealisedPnlPercent", totalTodayUnrealisedPnlPercent);

        return stats;


    }

    public List<Map<String, Object>> getHoldingStats(Long portfolioId) {

        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID must be provided");
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        List<Map<String, Object>> result = new ArrayList<>();

        Iterable<Holding> holdings =
                holdingRepository.findByPortfolioAndSellTimestampIsNullAndIsDeletedFalse(portfolio);

        for (Holding holding : (List<Holding>) holdings) {

            if (holding.getAsset() == null ||
                    holding.getQuantity() == null ||
                    holding.getAvgBuyPrice() == null) {
                continue;
            }

            BigDecimal quantity = holding.getQuantity();
            BigDecimal avgBuyPrice = holding.getAvgBuyPrice();
            BigDecimal currentPrice = getCurrentPrice(holding.getAsset());
            BigDecimal lastClose = getLastClosePrice(holding.getAsset());

            BigDecimal invested = avgBuyPrice.multiply(quantity);

            BigDecimal totalPL =
                    currentPrice.subtract(avgBuyPrice).multiply(quantity);

            BigDecimal todayPL =
                    currentPrice.subtract(lastClose).multiply(quantity);

            BigDecimal totalPLPercent = percent(totalPL, invested);
            BigDecimal todayPLPercent = percent(todayPL, invested);

            Map<String, Object> row = new HashMap<>();
            row.put("holdingId", holding.getId());
            row.put("asset", holding.getAsset().getAssetName()); // or getName()
            row.put("totalPL", totalPL);
            row.put("plPercent", totalPLPercent);
            row.put("todayPL", todayPL);
            row.put("todayPercent", todayPLPercent);

            result.add(row);
        }

        return result;
    }

    private BigDecimal getCurrentPrice(Asset asset) {
        AssetType assetType = asset.getAssetType();
        String assetName = asset.getAssetName();

        if (assetType == AssetType.STOCK) {
            return PriceFetcher.getStockPrice(assetName);
        } else if (assetType == AssetType.CRYPTOCURRENCY) {
            return PriceFetcher.getCryptoPrice(assetName);
        } else if (assetType == AssetType.COMMODITY) {
            return PriceFetcher.getCommodityPrice(assetName);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getLastClosePrice(Asset asset) {
        AssetType assetType = asset.getAssetType();
        String assetName = asset.getAssetName();

        if (assetType == AssetType.STOCK) {
            return PriceFetcher.getStockPriceLastClose(assetName);
        } else if (assetType == AssetType.CRYPTOCURRENCY) {
            return PriceFetcher.getCryptoPriceLastClose(assetName);
        } else if (assetType == AssetType.COMMODITY) {
            return PriceFetcher.getCommodityPriceLastClose(assetName);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 4, RoundingMode.HALF_UP);
    }
    
    

}

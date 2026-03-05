package com.orbit.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import com.orbit.portfolio.model.Asset;
import com.orbit.portfolio.model.Holding;
import com.orbit.portfolio.model.Portfolio;
import com.orbit.portfolio.model.Transaction;
import com.orbit.portfolio.model.enums.AssetType;
import com.orbit.portfolio.repository.*;

import org.springframework.stereotype.Service;


@Service
public class HoldingService {

	private final AssetRepository assetRepository;
    private final ClosePriceHistoryRepository closePriceHistoryRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public HoldingService(AssetRepository assetRepository,
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
    

        // create transaction
        private Asset resolveAsset(Asset asset) {
            if (asset == null) {
                throw new IllegalArgumentException("Asset must be provided for a transaction");
            }

            Optional<Asset> existing = assetRepository.findAll().stream()
                    .filter(a -> a.getAssetName() != null && a.getAssetType() != null)
                    .filter(a -> a.getAssetName().equalsIgnoreCase(asset.getAssetName())
                            && a.getAssetType() == asset.getAssetType())
                    .findFirst();

            return existing.orElseGet(() -> assetRepository.save(
                    new Asset(
                            asset.getAssetName(),
                            asset.getAssetType(),
                            asset.getAssetMetadata(),
                            asset.getUnit()
                    )
            ));
        }

        /////////////
        //   BUY   //
        /////////////

        public Transaction buyHolding(
                Long portfolioId,
                Asset asset,
                BigDecimal quantity,
                BigDecimal price,
                Instant buyTimestamp
        ) {
            if (asset == null) {
                throw new IllegalArgumentException("Asset must be provided for a transaction");
            }
            Asset resolvedAsset = resolveAsset(asset);

            if (portfolioId == null) {
                throw new IllegalArgumentException("Portfolio ID must be provided for a transaction");
            }
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be provided and greater than 0 for stock transactions");
            }

            Instant effectiveBuyTimestamp = (buyTimestamp == null) ? Instant.now() : buyTimestamp;

            BigDecimal avgBuyPrice;
            AssetType assetType = resolvedAsset.getAssetType();

            if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price must be greater than or equal to 0 for stock transactions");
            }

            if (assetType == AssetType.STOCK) {
                avgBuyPrice = (price == null)
                        ? PriceFetcher.getStockPrice(resolvedAsset.getAssetName())
                        : price;
            } else if (assetType == AssetType.CRYPTOCURRENCY) {
                avgBuyPrice = (price == null)
                        ? PriceFetcher.getCryptoPrice(resolvedAsset.getAssetName())
                        : price;
            } else if (assetType == AssetType.COMMODITY) {
                avgBuyPrice = (price == null)
                        ? PriceFetcher.getCommodityPrice(resolvedAsset.getAssetName())
                        : price;
            } else {
                throw new IllegalArgumentException("Unsupported asset type for transactions");
            }

            Holding holding = new Holding(
                    portfolio,
                    resolvedAsset,
                    quantity,
                    avgBuyPrice,
                    null,
                    effectiveBuyTimestamp,
                    null
            );
            holdingRepository.save(holding);

            Transaction transaction = Transaction.buy(
                    holding,
                    quantity,
                    avgBuyPrice,
                    effectiveBuyTimestamp
            );

            transactionRepository.save(transaction);
            return transaction;
        }


        /////////////
        //   SELL  //
        /////////////

        public Transaction sellHolding(
            Long portfolioId,
                Long holdingId,
                BigDecimal quantity,
                BigDecimal price,
                Instant sellTimestamp
        ) {

            BigDecimal avgSellPrice;
            
            if (portfolioId == null) {
                throw new IllegalArgumentException("Portfolio ID must be provided for a transaction");
            }
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));
            
            if (holdingId == null) {
                throw new IllegalArgumentException("Holding ID must be provided for a sell transaction");
            }
            Holding holding = holdingRepository.findById(holdingId)
                    .orElseThrow(() -> new IllegalArgumentException("Holding not found"));      
            
            

            Instant effectiveSellTimestamp = (sellTimestamp == null) ? Instant.now() : sellTimestamp;

            AssetType assetType = holding.getAsset().getAssetType();
            String assetNameString = holding.getAsset().getAssetName();

            if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price must be greater than or equal to 0 for stock transactions");
            }

            if (price == null) {
                if (assetType == AssetType.STOCK) {
                    avgSellPrice = PriceFetcher.getStockPrice(assetNameString);
                } else if (assetType == AssetType.CRYPTOCURRENCY) {
                    avgSellPrice = PriceFetcher.getCryptoPrice(assetNameString);
                } else if (assetType == AssetType.COMMODITY) {
                    avgSellPrice = PriceFetcher.getCommodityPrice(assetNameString);
                } else {
                    throw new IllegalArgumentException("Unsupported asset type for transactions");
                }
            } else {
                avgSellPrice = price;
            }

            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be provided and greater than 0 for stock transactions");
            }
            if(quantity.compareTo(holding.getQuantity()) > 0) {
                throw new IllegalArgumentException("Sell quantity cannot be greater than holding quantity");
            }
            if(quantity.compareTo(holding.getQuantity()) == 0) {
                //soft delete the holding                
                holding.setQuantity(BigDecimal.ZERO);
                holding.setAvgSellPrice(avgSellPrice);
                holding.setSellTimestamp(effectiveSellTimestamp);
                holdingRepository.save(holding);
            }
            else if(quantity.compareTo(holding.getQuantity()) < 0) {
                //if sell is partial i.e quantity x < holding quantity y then we need to create a new holding with the sell quantity x and update the existing holding to reduce the quantity by x
                Holding newHolding = new Holding(
                    portfolio,
                    holding.getAsset(),
                    quantity,
                    holding.getAvgBuyPrice(),
                    avgSellPrice,
                    holding.getBuyTimestamp(),
                    effectiveSellTimestamp
                );
                newHolding = holdingRepository.save(newHolding);
                holding.setQuantity(holding.getQuantity().subtract(quantity));  
                holdingRepository.save(holding);
            }

             Transaction transaction = Transaction.sell(
                    holding,
                    quantity,
                    holding.getAvgBuyPrice(),
                    holding.getBuyTimestamp(),
                    avgSellPrice,
                    effectiveSellTimestamp,
                    avgSellPrice.subtract(holding.getAvgBuyPrice()).multiply(quantity)
            );
            transactionRepository.save(transaction);
            return transaction;
        }



        /////////////
        //   EDIT  //
        /////////////
        

        public Transaction editHolding(
                Long portfolioId,
                Long holdingId,
                BigDecimal updatedQuantity,
                BigDecimal updatedBuyPrice
        ) {
            if (portfolioId == null) {
                throw new IllegalArgumentException("Portfolio ID must be provided for a transaction"); 
            }
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));
            if (holdingId == null) {
                throw new IllegalArgumentException("Holding ID must be provided for an edit transaction");
            }
            Holding holding = holdingRepository.findById(holdingId)
                    .orElseThrow(() -> new IllegalArgumentException("Holding not found"));
            if (updatedQuantity != null) {
                if (updatedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Updated quantity must be provided and greater than 0 for stock transactions");
                }
                holding.setQuantity(updatedQuantity);
            }

            if (updatedBuyPrice != null) {
                if (updatedBuyPrice.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Updated buy price must be provided and greater than or equal to 0 for stock transactions");
                }
                holding.setAvgBuyPrice(updatedBuyPrice);
            }

            holdingRepository.save(holding);

                Transaction transaction = Transaction.edit(
                    holding,
                    holding.getQuantity(),
                    holding.getAvgBuyPrice()
                );
            transactionRepository.save(transaction);
            return transaction;

        }


        ////////////////////
        //   SOFT DELETE  //
        ////////////////////
        
        public void deleteHolding(Long holdingId) {
            if (holdingId == null) {
                throw new IllegalArgumentException("Holding ID must be provided for deletion");
            }
            Holding holding = holdingRepository.findById(holdingId)
                    .orElseThrow(() -> new IllegalArgumentException("Holding not found"));
            
            //set holding.isDeleted to true
            holding.setDeleted(true);
            holdingRepository.save(holding);
            return;
        }



    ////////////////////////////////
    //   FETCH CURRENT HOLDINGS  //
    //////////////////////////////

//    public Iterable<Holding> getCurrentHoldingsByAssetType(Long portfolioId, String assetType) {
//        if (portfolioId == null) {
//            throw new IllegalArgumentException("Portfolio ID must be provided to fetch holdings");
//        }
//        Portfolio portfolio = portfolioRepository.findById(portfolioId)
//                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));
//
//        Iterable<Holding> allHoldings = holdingRepository.findByPortfolioAndSellTimestampIsNullAndIsDeletedFalse(portfolio);
//        return ((List<Holding>) allHoldings).stream()
//                .filter(holding -> holding.getAsset().getAssetType().toString().equalsIgnoreCase(assetType))
//                .toList();
//
//    }

    public List<Map<String, Object>> getCurrentHoldingsByAssetType(Long portfolioId, String assetType) {

        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID must be provided to fetch holdings");
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        Iterable<Holding> allHoldings =
                holdingRepository.findByPortfolioAndSellTimestampIsNullAndIsDeletedFalse(portfolio);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Holding holding : (List<Holding>) allHoldings) {

            if (!holding.getAsset().getAssetType().toString().equalsIgnoreCase(assetType)) {
                continue;
            }

            BigDecimal quantity = holding.getQuantity();
            BigDecimal avgBuyPrice = holding.getAvgBuyPrice();

            if (quantity == null || avgBuyPrice == null) {
                continue;
            }

            BigDecimal currentPrice = getCurrentPrice(holding.getAsset());
            BigDecimal lastClose = getLastClosePrice(holding.getAsset());

            BigDecimal invested = avgBuyPrice.multiply(quantity);

            BigDecimal totalPL =
                    currentPrice.subtract(avgBuyPrice).multiply(quantity);

            BigDecimal todayPL =
                    currentPrice.subtract(lastClose).multiply(quantity);

            BigDecimal totalPLPercent = percent(totalPL, lastClose.multiply(quantity));
            BigDecimal todayPLPercent = percent(todayPL, invested);

            Map<String, Object> row = new HashMap<>();

            // keep original holding
            row.put("portfolio", holding.getPortfolio());
            row.put("asset", holding.getAsset());
            row.put("quantity", holding.getQuantity());
            row.put("avgBuyPrice", holding.getAvgBuyPrice());
            row.put("avgSellPrice", holding.getAvgSellPrice());
            row.put("buyTimestamp", holding.getBuyTimestamp());
            row.put("sellTimestamp", holding.getSellTimestamp());
            row.put("createdAt", holding.getCreatedAt());
            row.put("deleted", holding.isDeleted());
            row.put("id", holding.getId());
            row.put("updatedAt", holding.getUpdatedAt());

            // add new computed fields
            row.put("totalPL", totalPL);
            row.put("plPercent", totalPLPercent);
            row.put("todayPL", todayPL);
            row.put("todayPercent", todayPLPercent);

            result.add(row);
        }

        return result;
    }


        ////////////////////////////////
        //   FETCH HISTORY HOLDINGS  //
        //////////////////////////////

        public Iterable<Holding> getHistoricalHoldingsByAssetType(Long portfolioId, String assetType) {
            if (portfolioId == null) {
                throw new IllegalArgumentException("Portfolio ID must be provided to fetch holdings");
            }
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

            Iterable<Holding> allHoldings = holdingRepository
                    .findByPortfolioAndSellTimestampIsNotNullAndIsDeletedFalse(portfolio);

            return ((List<Holding>) allHoldings).stream()
                    .filter(holding -> holding.getAsset().getAssetType().toString().equalsIgnoreCase(assetType))
                    .toList();
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
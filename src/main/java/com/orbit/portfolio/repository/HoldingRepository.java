package com.orbit.portfolio.repository;

import com.orbit.portfolio.model.Holding;
import com.orbit.portfolio.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    Optional<Holding> findByPortfolioIdAndAssetIdAndIsDeletedFalse(
            Long portfolioId,
            Long assetId
    );

    List<Holding> findByPortfolioIdAndIsDeletedFalse(Long portfolioId);

    Iterable<Holding> findByPortfolioAndSellTimestampIsNotNullAndIsDeletedFalse(Portfolio portfolio);

    Iterable<Holding> findByPortfolioAndSellTimestampIsNullAndIsDeletedFalse(Portfolio portfolio);
}

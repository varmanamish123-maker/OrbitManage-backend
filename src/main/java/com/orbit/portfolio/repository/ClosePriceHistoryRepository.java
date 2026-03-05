package com.orbit.portfolio.repository;

import com.orbit.portfolio.model.ClosePriceHistory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClosePriceHistoryRepository extends JpaRepository<ClosePriceHistory, Long> {

    Optional<ClosePriceHistory> findTopByAsset_AssetNameOrderByClosePriceTimestampDesc(String assetName);
}

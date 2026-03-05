package com.orbit.portfolio.repository;

import com.orbit.portfolio.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
	Optional<Asset> findByAssetName(String assetName);
}

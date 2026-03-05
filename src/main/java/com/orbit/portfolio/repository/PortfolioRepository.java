package com.orbit.portfolio.repository;

import com.orbit.portfolio.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUserIdAndIsDeletedFalse(Long userId);
}

package com.orbit.portfolio.repository;

import com.orbit.portfolio.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByHoldingIdAndIsDeletedFalseOrderByExecutedAtAsc(Long holdingId);
}

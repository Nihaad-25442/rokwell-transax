package com.rockwell.transax.repository;

import com.rockwell.transax.entity.TransactionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionBatchRepository extends JpaRepository<TransactionBatch, Long> {

    Optional<TransactionBatch> findByBatchReference(String batchReference);
}

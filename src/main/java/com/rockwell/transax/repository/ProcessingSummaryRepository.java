package com.rockwell.transax.repository;

import com.rockwell.transax.entity.ProcessingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingSummaryRepository extends JpaRepository<ProcessingSummary, Long> {
}

package com.rockwell.transax.repository;

import com.rockwell.transax.entity.ProcessingError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingErrorRepository extends JpaRepository<ProcessingError, Long> {
}

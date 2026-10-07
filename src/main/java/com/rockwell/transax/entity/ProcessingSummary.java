package com.rockwell.transax.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "processing_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingSummary extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long summaryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, unique = true)
    private TransactionBatch batch;

    @Column(nullable = false)
    private Integer totalRecords;

    @Column(nullable = false)
    private Integer successfulRecords;

    @Column(nullable = false)
    private Integer failedRecords;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal successfulAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal failedAmount;

    private Long processingDuration;
}

package com.rockwell.transax.entity;

import com.rockwell.transax.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "transaction_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long batchId;

    @Column(nullable = false, unique = true, length = 100)
    private String batchReference;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(length = 500)
    private String filePath;

    private Integer totalRecords;

    private Integer successfulRecords;

    private Integer failedRecords;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus batchStatus;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "batch")
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToOne(mappedBy = "batch", cascade = CascadeType.ALL)
    private ProcessingSummary processingSummary;

}

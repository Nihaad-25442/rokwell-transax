package com.rockwell.transax.batch;

import com.rockwell.transax.entity.TransactionBatch;
import com.rockwell.transax.enums.BatchStatus;
import com.rockwell.transax.repository.TransactionBatchRepository;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TransactionBatchJobListener implements JobExecutionListener {

    private final TransactionBatchRepository transactionBatchRepository;

    public TransactionBatchJobListener(
            TransactionBatchRepository transactionBatchRepository
    ) {
        this.transactionBatchRepository = transactionBatchRepository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        TransactionBatch batch = TransactionBatch.builder()
                .batchReference("TRAINING-BATCH-" + jobExecution.getId())
                .filename("transactions.csv")
                .filePath("data/transactions.csv")
                .batchStatus(BatchStatus.PROCESSING)
                .startedAt(LocalDateTime.now())
                .totalRecords(0)
                .successfulRecords(0)
                .failedRecords(0)
                .build();

        transactionBatchRepository.save(batch);

        jobExecution.getExecutionContext().putString("batchReference", batch.getBatchReference());

        System.out.println("Created TransactionBatch: "
                                   + batch.getBatchReference()
        );
    }
}

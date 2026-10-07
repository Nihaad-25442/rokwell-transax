package com.rockwell.transax.config;

import com.rockwell.transax.batch.TransactionBatchJobListener;
import com.rockwell.transax.batch.dto.TransactionInput;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;

//import org.springframework.batch.infrastructure.repeat.RepeatStatus;
//import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
public class BatchConfig {

    @Bean
    public Step demoStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<TransactionInput> transactionReader,
            ItemProcessor<TransactionInput, TransactionInput> transactionProcessor,
            ItemWriter<TransactionInput> transactionWriter
    ) {
        return new StepBuilder("demoStep", jobRepository)
                .<TransactionInput, TransactionInput>chunk(3)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public Job demoJob(
            JobRepository jobRepository,
            Step demoStep,
            TransactionBatchJobListener transactionBatchJobListener
    ) {
        return new JobBuilder("demoJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(transactionBatchJobListener)
                .start(demoStep)
                .build();
    }

    @Bean
    public FlatFileItemReader<TransactionInput> transactionReader() {
        return new FlatFileItemReaderBuilder<TransactionInput>()
                .name("transactionReader")
                .resource(new ClassPathResource("data/transactions.csv"))
                .linesToSkip(1)
                .delimited()
                .names(
                        "transactionReference",
                        "accountNumber",
                        "transactionType",
                        "amount",
                        "currency"
                )
                .targetType(TransactionInput.class)
                .build();
    }

    @Bean
    public ItemProcessor<TransactionInput, TransactionInput> transactionProcessor() {
        return item -> {
            System.out.println(
                    "Processing: " + item.getTransactionReference()
            );
            return item;
        };
    }

    @Bean
    public ItemWriter<TransactionInput> transactionWriter() {
        return chunk -> {
            for (TransactionInput item : chunk) {
                System.out.println(
                        "Writing: " + item.getTransactionReference()
                        + " | Account: " + item.getAccountNumber()
                        + " | Type: " + item.getTransactionType()
                        + " | Amount: " + item.getAmount()
                        + " | Currency: " + item.getCurrency()
                );
            }
        };
    }



}

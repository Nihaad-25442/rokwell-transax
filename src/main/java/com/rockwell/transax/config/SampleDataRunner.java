package com.rockwell.transax.config;

import com.rockwell.transax.entity.Account;
import com.rockwell.transax.entity.Customer;
import com.rockwell.transax.entity.Transaction;
import com.rockwell.transax.entity.TransactionBatch;
import com.rockwell.transax.enums.AccountStatus;
import com.rockwell.transax.enums.AccountType;
import com.rockwell.transax.enums.BatchStatus;
import com.rockwell.transax.enums.CustomerStatus;
import com.rockwell.transax.enums.TransactionStatus;
import com.rockwell.transax.enums.TransactionType;
import com.rockwell.transax.repository.AccountRepository;
import com.rockwell.transax.repository.CustomerRepository;
import com.rockwell.transax.repository.TransactionBatchRepository;
import com.rockwell.transax.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "transax.startup.sample-data.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SampleDataRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataRunner.class);
    private static final String SAMPLE_BATCH_REFERENCE = "SAMPLE-BATCH-2026-001";

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionBatchRepository transactionBatchRepository;
    private final TransactionRepository transactionRepository;
    private final EntityManager entityManager;

    public SampleDataRunner(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionBatchRepository transactionBatchRepository,
            TransactionRepository transactionRepository,
            EntityManager entityManager) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionBatchRepository = transactionBatchRepository;
        this.transactionRepository = transactionRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (transactionBatchRepository.findByBatchReference(SAMPLE_BATCH_REFERENCE).isPresent()) {
            log.info("Sample data already exists; skipping creation");
            return;
        }

        Customer customer = customerRepository.saveAndFlush(Customer.builder()
                .customerNumber("CUST-SAMPLE-001")
                .firstName("Amina")
                .lastName("Rahman")
                .email("amina.rahman@example.com")
                .phone("+971500000001")
                .status(CustomerStatus.ACTIVE)
                .build());

        Account checkingAccount = Account.builder()
                .accountNumber("ACC-SAMPLE-001")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1500.00"))
                .currency("MUR")
                .status(AccountStatus.ACTIVE)
                .customer(customer)
                .build();
        Account savingsAccount = Account.builder()
                .accountNumber("ACC-SAMPLE-002")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .currency("MUR")
                .status(AccountStatus.ACTIVE)
                .customer(customer)
                .build();
        customer.getAccounts().addAll(List.of(checkingAccount, savingsAccount));
        accountRepository.saveAll(List.of(checkingAccount, savingsAccount));
        accountRepository.flush();

        TransactionBatch batch = transactionBatchRepository.saveAndFlush(TransactionBatch.builder()
                .batchReference(SAMPLE_BATCH_REFERENCE)
                .filename("sample-transactions.csv")
                .filePath("/data/samples/sample-transactions.csv")
                .totalRecords(3)
                .successfulRecords(3)
                .failedRecords(0)
                .batchStatus(BatchStatus.COMPLETED)
                .build());

        Transaction credit = createTransaction(
                "TX-SAMPLE-001", checkingAccount, batch, TransactionType.CREDIT,
                new BigDecimal("250.00"), "Salary credit");
        Transaction payment = createTransaction(
                "TX-SAMPLE-002", checkingAccount, batch, TransactionType.PAYMENT,
                new BigDecimal("75.50"), "Utility payment");
        Transaction transfer = createTransaction(
                "TX-SAMPLE-003", savingsAccount, batch, TransactionType.CREDIT,
                new BigDecimal("1000.00"), "Savings deposit");

        batch.getTransactions().addAll(List.of(credit, payment, transfer));
        checkingAccount.getTransactions().addAll(List.of(credit, payment));
        savingsAccount.getTransactions().add(transfer);
        transactionRepository.saveAll(List.of(credit, payment, transfer));
        transactionRepository.flush();

        entityManager.clear();
        verifyPersistedRelationships(batch.getBatchId(), checkingAccount.getAccountId());
    }

    private Transaction createTransaction(
            String reference,
            Account account,
            TransactionBatch batch,
            TransactionType type,
            BigDecimal amount,
            String description) {
        return Transaction.builder()
                .transactionReference(reference)
                .account(account)
                .batch(batch)
                .transactionType(type)
                .amount(amount)
                .currency("MUR")
                .description(description)
                .status(TransactionStatus.PROCESSED)
                .build();
    }

    private void verifyPersistedRelationships(Long batchId, Long accountId) {
        TransactionBatch persistedBatch = transactionBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Sample transaction batch was not persisted"));
        Account persistedAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Sample account was not persisted"));

        long batchTransactions = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getBatch().getBatchId().equals(persistedBatch.getBatchId()))
                .count();
        long accountTransactions = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getAccount().getAccountId().equals(persistedAccount.getAccountId()))
                .count();

        if (batchTransactions != 3 || accountTransactions != 2) {
            throw new IllegalStateException("Sample transaction relationships were not persisted correctly");
        }
        log.info("Persisted sample data: batch {} has {} transactions; account {} has {} transactions",
                persistedBatch.getBatchReference(), batchTransactions,
                persistedAccount.getAccountNumber(), accountTransactions);
    }
}

package com.rockwell.transax.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TransactionProcessingServiceTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionBatchRepository transactionBatchRepository;

    @Autowired
    private TransactionProcessingService transactionProcessingService;

    private Account account;
    private TransactionBatch batch;

    @BeforeEach
    void setUp() {
        Customer customer = customerRepository.saveAndFlush(Customer.builder()
                .customerNumber("CUST-TEST-001")
                .firstName("Test")
                .lastName("User")
                .email("test.user+" + UUID.randomUUID() + "@example.com")
                .phone("+123456789")
                .status(CustomerStatus.ACTIVE)
                .build());

        account = accountRepository.saveAndFlush(Account.builder()
                .accountNumber("ACC-TEST-001")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1500.00"))
                .currency("MUR")
                .status(AccountStatus.ACTIVE)
                .customer(customer)
                .build());

        batch = transactionBatchRepository.saveAndFlush(TransactionBatch.builder()
                .batchReference("BATCH-" + UUID.randomUUID())
                .filename("test-batch.csv")
                .filePath("/tmp/test-batch.csv")
                .totalRecords(1)
                .successfulRecords(1)
                .failedRecords(0)
                .batchStatus(BatchStatus.COMPLETED)
                .build());
    }

    @Test
    void processValidCreditIncreasesBalance() {
        Transaction transaction = createTransaction(
                "TX-VALID-CREDIT-001",
                new BigDecimal("250.00"),
                TransactionType.CREDIT
        );

        TransactionProcessingService.ProcessingResult result = transactionProcessingService.process(transaction);

        assertThat(result.successful()).isTrue();
        assertThat(result.status()).isEqualTo(TransactionStatus.PROCESSED);
        assertThat(result.errors()).isEmpty();

        Account refreshedAccount = accountRepository.findByAccountNumber(account.getAccountNumber()).orElseThrow();
        assertThat(refreshedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1750.00"));
    }

    @Test
    void validateRejectsDuplicateReference() {
        String duplicateReference = "TX-DUPLICATE-001";
        transactionRepository.saveAndFlush(createTransaction(duplicateReference, new BigDecimal("100.00"), TransactionType.CREDIT));

        Transaction duplicate = createTransaction(duplicateReference, new BigDecimal("50.00"), TransactionType.CREDIT);

        TransactionProcessingService.ValidationResult result = transactionProcessingService.validate(duplicate);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .extracting(TransactionProcessingService.ValidationError::code)
                .contains("DUPLICATE_REFERENCE");
    }

    @Test
    void processRejectsInsufficientBalance() {
        Transaction transaction = createTransaction(
                "TX-INSUFFICIENT-BALANCE-001",
                new BigDecimal("2000.00"),
                TransactionType.DEBIT
        );

        TransactionProcessingService.ProcessingResult result = transactionProcessingService.process(transaction);

        assertThat(result.successful()).isFalse();
        assertThat(result.status()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.errors())
                .extracting(TransactionProcessingService.ProcessingErrorDetail::code)
                .contains("INSUFFICIENT_BALANCE");
    }

    private Transaction createTransaction(String reference, BigDecimal amount, TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setTransactionReference(reference);
        transaction.setAccount(account);
        transaction.setBatch(batch);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrency("MUR");
        transaction.setStatus(TransactionStatus.PENDING);
        return transaction;
    }
}

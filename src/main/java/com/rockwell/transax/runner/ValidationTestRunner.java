package com.rockwell.transax.runner;

import com.rockwell.transax.entity.Account;
import com.rockwell.transax.entity.Transaction;
import com.rockwell.transax.entity.TransactionBatch;
import com.rockwell.transax.enums.TransactionType;
import com.rockwell.transax.repository.AccountRepository;
import com.rockwell.transax.service.TransactionProcessingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rockwell.transax.enums.TransactionStatus;
import com.rockwell.transax.service.TransactionProcessingService.ProcessingResult;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(
        name = "transax.validation.runner.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ValidationTestRunner implements CommandLineRunner {
    private final TransactionProcessingService transactionProcessingService;
    private final AccountRepository accountRepository;

    public ValidationTestRunner(
            TransactionProcessingService transactionProcessingService,
            AccountRepository accountRepository
    ) {
        this.transactionProcessingService = transactionProcessingService;
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n========================================");
        System.out.println("TRANSACTION VALIDATION MANUAL TESTS");
        System.out.println("========================================");

        testValidTransaction();
        testMissingReference();
        testDuplicateReference();
        testMissingAmount();
        testNegativeAmount();
        testMissingAccount();
        testNonExistentAccount();
        testMissingTransactionType();
        testUnsupportedCurrency();
        testInvalidIsoCurrency();
        testNullTransaction();

        System.out.println("========================================");
        System.out.println("VALIDATION TESTS COMPLETED");
        System.out.println("========================================\n");

        System.out.println("\n========================================");
        System.out.println("TRANSACTION PROCESSING MANUAL TESTS");
        System.out.println("========================================");

        testCreditTransaction();

        System.out.println("========================================");
        System.out.println("PROCESSING TESTS COMPLETED");
        System.out.println("========================================\n");
    }

    private void testValidTransaction() {
        Account account = createAccount("ACC-SAMPLE-001");

        Transaction transaction = createTransaction(
                "TEST-VALID-001",
                new BigDecimal("100.00"),
                account,
                TransactionType.CREDIT,
                "MUR"
        );

        printResult(
                "TEST 1 - Valid Transaction",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testMissingReference() {
        Account account = createAccount("ACC-SAMPLE-001");

        Transaction transaction = createTransaction(
                "",
                new BigDecimal("100.00"),
                account,
                TransactionType.CREDIT,
                "MUR"
        );

        printResult(
                "TEST 2 - Missing Reference",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testDuplicateReference() {
        Account account = createAccount("ACC-SAMPLE-001");
        /*
         * This reference should already exist in the database
         * For this test to work, the reference should be an existing one
         */
        Transaction transaction = createTransaction(
                "TX-SAMPLE-001",
                new BigDecimal("100.00"),
                account,
                TransactionType.CREDIT,
                "MUR"
        );

        printResult(
                "TEST 3 - Duplicate Reference",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testMissingAmount() {
        Account account = createAccount("ACC-SAMPLE-001");
         Transaction transaction = createTransaction(
                 "TEST-MISSING-AMOUNT-001",
                 null,
                 account,
                 TransactionType.CREDIT,
                 "MUR"
         );

         printResult(
                 "TEST 4 - Missing Amount",
                 transactionProcessingService.validate(transaction)
         );
    }

    private void testNegativeAmount() {
        Account account = createAccount("ACC-SAMPLE-001");

        Transaction transaction = createTransaction(
                "TEST-NEGATIVE-AMOUNT-001",
                new BigDecimal("-50.00"),
                account,
                TransactionType.DEBIT,
                "MUR"
        );

        printResult(
                "TEST 5 - Negative Amount",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testMissingAccount() {
        Transaction transaction = createTransaction(
                "TEST-MISSING-ACCOUNT-001",
                new BigDecimal("100.00"),
                null,
                TransactionType.CREDIT,
                "MUR"
        );

        printResult(
                "TEST 6 - Missing Account",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testNonExistentAccount() {
        Account account = createAccount("ACC-DOES-NOT-EXIST");

        Transaction transaction = createTransaction(
                "TEST-INVALID-ACCOUNT-001",
                new BigDecimal("100.00"),
                account,
                TransactionType.CREDIT,
                "MUR"
        );

        printResult(
                "TEST 7 - Non-Existent Account",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testMissingTransactionType() {
        Account account = createAccount("ACC-SAMPLE-001");

        Transaction transaction = createTransaction(
          "TEST-MISSING-TYPE-001",
          new BigDecimal("100.00"),
          account,
          null,
          "MUR"
        );

        printResult(
                "Test 8 - Missing TransactionType",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testUnsupportedCurrency() {
        Account account = createAccount("ACC-SAMPLE-001");

        Transaction transaction = createTransaction(
                "TEST-UNSUPPORTED-CURRENCY-001",
                new BigDecimal("100.00"),
                account,
                TransactionType.CREDIT,
                "USD"
        );

        printResult(
                "TEST 9 - Unsupported currency (USD)",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testInvalidIsoCurrency() {
        Account account = createAccount("ACC-SAMPLE-001");

        Transaction transaction = createTransaction(
                "TEST-INVALID-ISO-CURRENCY-001",
                new BigDecimal("100.00"),
                account,
                TransactionType.CREDIT,
                "XYZ"
        );

        printResult(
                "TEST 10 - Invalid currency (XYZ)",
                transactionProcessingService.validate(transaction)
        );
    }

    private void testNullTransaction() {
        printResult(
                "TEST 11 - Null transaction",
                transactionProcessingService.validate(null)
        );
    }

    private void testCreditTransaction() {
        Account account = accountRepository
                .findByAccountNumber("ACC-SAMPLE-001").orElseThrow(() ->
                        new IllegalStateException("Test account not found")
                );

        BigDecimal balanceBefore = account.getBalance();

        Transaction transaction = createTransaction(
                "TEST-PROCESS-CREDIT-001",
                new BigDecimal("250.00"),
                account,
                TransactionType.CREDIT,
                "MUR"
        );

        transaction.setStatus(TransactionStatus.PENDING);

        ProcessingResult result = transactionProcessingService.process(transaction);

        Account updatedAccount = accountRepository
                .findByAccountNumber("ACC-SAMPLE-001")
                .orElseThrow();

        System.out.println("\n--- TEST 1 - CREDIT TRANSACTION ---");

        System.out.println("Balance before: " + balanceBefore);
        System.out.println("Transaction amount: " + transaction.getAmount());
        System.out.println("Balance after: " + updatedAccount.getBalance());

        System.out.println("Successful:        " + result.successful());
        System.out.println("Status:            " + result.status());
        System.out.println("Reference:         " + result.transactionReference());
        System.out.println("Account:           " + result.accountNumber());
        System.out.println("Previous balance:  " + result.previousBalance());
        System.out.println("Resulting balance: " + result.resultingBalance());

        if (result.errors().isEmpty()) {
            System.out.println("Errors:            ");
        } else {
            System.out.println("Errors: ");

            result.errors().forEach(error ->
                            System.out.println(
                                    " [" + error.code() + "] " + error.message()
                            )
                    );
        }
    }

    private Transaction createTransaction(
            String reference,
            BigDecimal amount,
            Account account,
            TransactionType type,
            String currency
    ) {
        Transaction transaction = new Transaction();

        transaction.setTransactionReference(reference);
        transaction.setAmount(amount);
        transaction.setAccount(account);
        transaction.setBatch(new TransactionBatch());
        transaction.setTransactionType(type);
        transaction.setCurrency(currency);

        return transaction;
    }

    private Account createAccount(String accountNumber) {
        Account account = new Account();

        account.setAccountNumber(accountNumber);

        return account;
    }

    private void printResult(
            String testName,
            TransactionProcessingService.ValidationResult result
    ) {
        System.out.println("\n--- " + testName + " ---");
        System.out.println("Valid:  " + result.valid());

        if (result.errors().isEmpty()) {
            System.out.println("Errors: NONE");
        } else {
            System.out.println("Errors:");

            result.errors().forEach(error ->
                            System.out.println(
                                    " [" + error.code() + "] " + error.message()
                            )
                    );
        }
    }


}

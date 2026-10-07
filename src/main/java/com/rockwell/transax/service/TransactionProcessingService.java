package com.rockwell.transax.service;

import com.rockwell.transax.entity.Account;
import com.rockwell.transax.entity.Transaction;
import com.rockwell.transax.enums.AccountStatus;
import com.rockwell.transax.enums.ErrorType;
import com.rockwell.transax.enums.TransactionStatus;
import com.rockwell.transax.repository.AccountRepository;
import com.rockwell.transax.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service // It tells spring that this is class is apllication/business logic & to manage it as a Spring bean
@Transactional(readOnly = true)
public class TransactionProcessingService {
    /// The two current database dependencies
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionProcessingService (AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public ProcessingResult process(Transaction transaction) {
        ValidationResult validation = validate(transaction);
        String transactionReference = transaction == null
                ? null
                : transaction.getTransactionReference();
        String accountNumber = transaction == null || transaction.getAccount() == null
                ? null
                : transaction.getAccount().getAccountNumber();

        if(!validation.valid()){
            return new ProcessingResult(
                    false,
                    TransactionStatus.REJECTED,
                    transactionReference,
                    accountNumber,
                    null,
                    null,
                    validation.errors().stream()
                            .map(error -> new ProcessingErrorDetail(
                            error.code(),
                            error.message(),
                            ErrorType.VALIDATION
                    ))
                    .toList()
            );
        }

        Account account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalStateException (
                        "Account disappeared after validation: " + accountNumber
                ));

        transaction.setStatus(TransactionStatus.PROCESSING);

        BigDecimal previousBalance = account.getBalance();
        BigDecimal amount = transaction.getAmount();

        boolean debit = switch (transaction.getTransactionType()) {
            case PAYMENT, DEBIT -> true;
            case CREDIT, REFUND -> false;
        };

        if (debit && amount.compareTo(previousBalance) > 0) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transactionRepository.save(transaction);

            return new ProcessingResult(
                    false,
                    TransactionStatus.REJECTED,
                    transactionReference,
                    account.getAccountNumber(),
                    previousBalance,
                    previousBalance,
                    List.of(new ProcessingErrorDetail(
                            "INSUFFICIENT_BALANCE",
                            "Account balance is insufficient for this transaction",
                            ErrorType.PROCESSING
                    ))
            );
        }

        BigDecimal resultingBalance = switch (transaction.getTransactionType()) {
            case CREDIT, REFUND -> previousBalance.add(amount);
            case PAYMENT, DEBIT -> previousBalance.subtract(amount);
        };

        account.setBalance(resultingBalance);
        accountRepository.save(account);

        transaction.setStatus(TransactionStatus.PROCESSED);
        transactionRepository.save(transaction);

        return new ProcessingResult(
                true,
                TransactionStatus.PROCESSED,
                transactionReference,
                account.getAccountNumber(),
                previousBalance,
                resultingBalance,
                List.of()
        );
    }

    public ValidationResult validate(Transaction transaction) {
        List<ValidationError> errors = new ArrayList<>();

        if (transaction == null) {
            errors.add(new ValidationError("TRANSACTION_REQUIRED", "Transaction is required"));
            return new ValidationResult(false, errors);
        }

        if (isBlank(transaction.getTransactionReference())) {
            errors.add(new ValidationError("REFERENCE_REQUIRED", "Transaction reference is required"));
        } else if (transactionRepository.findByTransactionReference((transaction.getTransactionReference())).isPresent()) {
            errors.add(new ValidationError("DUPLICATE_REFERENCE", "Transaction reference already exists"));
        }

        BigDecimal amount = transaction.getAmount();
        if (amount == null) {
            errors.add(new ValidationError("AMOUNT_REQUIRED", "Transaction amount is required"));
        } else if (amount.signum() <= 0) {
            errors.add(new ValidationError("AMOUNT_INVALID", "Transaction amount must be greater than zero"));
        }

        if (transaction.getTransactionType() == null) {
            errors.add(new ValidationError("TYPE_REQUIRED", "Transaction type is required"));
        }

        validateCurrency(transaction.getCurrency(), errors);

        if (transaction.getAccount() == null || isBlank(transaction.getAccount().getAccountNumber())) {
            errors.add(new ValidationError("ACCOUNT_REQUIRED", "Customer Account is required"));
        } else {
            Optional<Account> account = accountRepository.findByAccountNumber(transaction.getAccount().getAccountNumber());

            if (account.isEmpty()) {
                errors.add(new ValidationError("ACCOUNT_INVALID", "Account does not exist"));
            } else {
                Account retrievedAccount = account.get();

                if (retrievedAccount.getStatus() != AccountStatus.ACTIVE) {
                    errors.add(new ValidationError("ACCOUNT_INACTIVE", "Account is not active"));
                }

                if (!isBlank(transaction.getCurrency())
                        && isValidCurrency(transaction.getCurrency())
                        && !transaction.getCurrency().equals(retrievedAccount.getCurrency())) {
                    errors.add(new ValidationError(
                            "CURRENCY_MISMATCH",
                            "Transaction currency does not match account currency"
                    ));
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private void validateCurrency(String currencyCode, List<ValidationError> errors) {
        if (isBlank(currencyCode)) {
            errors.add(new ValidationError("CURRENCY_REQUIRED", "Currency is required"));
            return;
        }

        if (!isValidCurrency(currencyCode)) {
            errors.add(new ValidationError("CURRENCY_INVALID", "Currency must be a valid ISO 4217 code"));
            return;
        }

        if (!currencyCode.equals("MUR")) {
            errors.add(new ValidationError(
                    "CURRENCY_UNSUPPORTED",
                    "Transaction currency must be MUR"
            ));
        }
    }

    private boolean isValidCurrency(String currencyCode) {
        if (!currencyCode.equals(currencyCode.toUpperCase(Locale.ROOT))) {
            return false;
        }
        try {
            Currency.getInstance(currencyCode);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ValidationResult(boolean valid, List<ValidationError> errors) {

        public ValidationResult {
            errors = List.copyOf(errors);
        }
    }

    public record ValidationError(String code, String message) {
    }

    public record ProcessingResult(
            boolean successful,
            TransactionStatus status,
            String transactionReference,
            String accountNumber,
            BigDecimal previousBalance,
            BigDecimal resultingBalance,
            List<ProcessingErrorDetail> errors
    ) {
        public ProcessingResult {
            errors = List.copyOf(errors);
        }
    }

    public record ProcessingErrorDetail(
            String code,
            String message,
            ErrorType type
    ) {
    }

}

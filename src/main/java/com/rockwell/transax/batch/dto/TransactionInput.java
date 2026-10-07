package com.rockwell.transax.batch.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionInput {

    private String transactionReference;
    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
    private String currency;

}

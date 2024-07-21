package com.mg.financial.transaction.analysis.web.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
public class TransactionSummary {
    private int id;
    private BigDecimal totalAmount;
    private Timestamp timestamp;
}


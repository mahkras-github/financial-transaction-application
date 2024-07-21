package com.mg.financial.transaction.analysis.web.service;

import com.mg.financial.transaction.analysis.web.model.TransactionSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TransactionSummaryRowMapper implements RowMapper<TransactionSummary> {
    @Override
    public TransactionSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        TransactionSummary summary = new TransactionSummary();
        summary.setId(rs.getInt("id"));
        summary.setTotalAmount(rs.getBigDecimal("total_amount"));
        summary.setTimestamp(rs.getTimestamp("timestamp"));
        return summary;
    }
}


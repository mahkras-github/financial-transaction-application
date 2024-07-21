package com.mg.financial.transaction.analysis.web.dao;

import com.mg.financial.transaction.analysis.web.model.TransactionSummary;
import com.mg.financial.transaction.analysis.web.service.TransactionSummaryRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransactionSummaryDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionSummaryRowMapper rowMapper;

    public List<TransactionSummary> getAllTransactionSummaries() {
        String sql = "SELECT * FROM transaction_summary ORDER BY timestamp DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }
}


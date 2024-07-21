package com.mg.financial.transaction.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionAnalysisTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
    @BeforeEach
    public void setup() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // Insert sample data into transactions table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO transactions (transactionId, fromAccountId, toAccountId, amount, transactionDate, transactionType) " +
                            "VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, "1");
                stmt.setString(2, "acc1");
                stmt.setString(3, "acc2");
                stmt.setBigDecimal(4, new BigDecimal("100.00"));
                stmt.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
                stmt.setString(6, "Transfer");
                stmt.executeUpdate();
            }

            // Insert sample data into transaction_summary table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO transaction_summary (total_amount) VALUES (?)")) {
                stmt.setBigDecimal(1, new BigDecimal("100.00"));
                stmt.executeUpdate();
            }
        }
    }

     */

    @Test
    void testTransactionsTableIsNotEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertTrue(count > 0, "Transactions table should not be empty");
    }

    @Test
    void testTransactionSummaryTableIsNotEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transaction_summary", Integer.class);
        assertTrue(count > 0, "Transaction summary table should not be empty");
    }
}


package com.mg.financial.transaction.analysis.web.controller;

import com.mg.financial.transaction.analysis.web.dao.TransactionSummaryDao;
import com.mg.financial.transaction.analysis.web.model.TransactionSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private TransactionSummaryDao transactionSummaryDao;

    @GetMapping("/analytics")
    public List<TransactionSummary> getAnalytics() {
        return transactionSummaryDao.getAllTransactionSummaries();
    }
}


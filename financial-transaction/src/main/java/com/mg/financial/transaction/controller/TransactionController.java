package com.mg.financial.transaction.controller;

import com.mg.financial.transaction.model.Transaction;
import com.mg.financial.transaction.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @PostMapping
    public String sendTransaction(@RequestBody Transaction transaction) {
        kafkaProducerService.sendTransaction(transaction);
        return "Transaction sent successfully";
    }
}


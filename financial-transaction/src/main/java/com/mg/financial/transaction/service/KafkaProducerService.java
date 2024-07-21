package com.mg.financial.transaction.service;

import com.mg.financial.transaction.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String TOPIC = "transactions";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTransaction(Transaction transaction) {
        kafkaTemplate.send(TOPIC, transaction);
    }
}

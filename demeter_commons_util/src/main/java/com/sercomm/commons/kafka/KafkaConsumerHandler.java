package com.sercomm.commons.kafka;

import java.util.Map;

public interface KafkaConsumerHandler
{
    void ConsumerMessageReceived(
            KafkaConsumerClient kafkaConsumerClient,
            String topic,
            int partition,
            long offset,
            Map<String, byte[]> headers,
            byte[] message);
    void ConsumerErrorOccurred(
            KafkaConsumerClient kafkaConsumerClient,
            Throwable t);
    void ConsumerClosed(
            KafkaConsumerClient kafkaConsumerClient);
    void ConsumerPartitionAssigned(
            String topic, 
            int partition);
    void ConsumerPartitionRevoked(
            String topic, 
            int partition);
}

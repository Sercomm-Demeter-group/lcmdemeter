package com.sercomm.commons.kafka;

public interface KafkaProducerHandler
{
    void ProducerMessageDelivering(
            KafkaProducerClient kafkaProducerClient,
            String topic,
            byte[] message);
    void ProducerMessageDeliveredResult(
            KafkaProducerClient kafkaProducerClient,
            Exception e,
            String topic,
            int partition,
            long offset,
            byte[] message);
    void ProducerErrorOccurred(
            KafkaProducerClient kafkaProducerClient,
            Throwable t);
    void ProducerClosed(
            KafkaProducerClient kafkaProducerClient);
}

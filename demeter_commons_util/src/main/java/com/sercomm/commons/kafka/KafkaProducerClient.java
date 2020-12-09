package com.sercomm.commons.kafka;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public final class KafkaProducerClient implements Closeable
{
    private KafkaProducer<byte[], byte[]> producer;
    private KafkaProducerHandler handler;
    
    private KafkaProducerClient(Properties properties, KafkaProducerHandler kafkaProducerHandler)
    {
        this.handler = kafkaProducerHandler;
        this.producer = new KafkaProducer<byte[], byte[]>(properties);
    }

    public synchronized void deliver(
            String topic, 
            Map<String, byte[]> headers, 
            byte[] message)
    {        
        this.deliver(topic, null, headers, message);
    }
    
    public synchronized void deliver(
            String topic, 
            Integer partition, 
            Map<String, byte[]> headers, 
            byte[] message)
    {
        if(null == this.producer)
        {
            throw new IllegalStateException();
        }
        
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<byte[], byte[]>(
                topic,                  // topic
                partition,              // partition
                null,                   // key
                message);               // value

        org.apache.kafka.common.header.Headers collection = record.headers();
        if(null != headers)
        {
            for(Map.Entry<String, byte[]> entry : headers.entrySet())
            {
                collection.add(entry.getKey(), entry.getValue());
            }
        }
        
        try
        {
            if(null != this.handler)
            {
                this.handler.ProducerMessageDelivering(this, topic, message);
            }

            final KafkaProducerHandler handler = this.handler;
            this.producer.send(record, new Callback() 
            {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e)
                {
                    if(null != handler)
                    {
                        handler.ProducerMessageDeliveredResult(
                            KafkaProducerClient.this, 
                            e, 
                            topic, 
                            null == e ? recordMetadata.partition() : -1, 
                            null == e ? recordMetadata.offset() : -1L, 
                            message);
                    }
                }
            });
        }
        catch(Throwable t)
        {
            if(null != this.handler)
            {
                this.handler.ProducerErrorOccurred(this, t);
            }
        }
    }
    
    @Override
    public synchronized void close() throws IOException
    {
        if(null != this.producer)
        {
            //this.producer.close(Duration.ofMillis(1000L));
            this.producer.close();
            
            if(null != this.handler)
            {
                this.handler.ProducerClosed(this);
            }
        }
    }
    
    public static class Builder
    {
        private Properties properties;
        private KafkaProducerHandler handler;
    
        public Builder()
        {
            this.properties = new Properties();
            
            this.properties.put(ProducerConfig.ACKS_CONFIG, "all");
            this.properties.put(ProducerConfig.RETRIES_CONFIG, 0);
            this.properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
            this.properties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
            this.properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
            this.properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
            this.properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        }
        
        public KafkaProducerClient build()
        {
            KafkaProducerClient object = new KafkaProducerClient(this.properties, this.handler);
            return object;
        }
        
        public Builder bootstrapServer(String address)
        {
            this.properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, address);
            return this;
        }
        
        public Builder handler(KafkaProducerHandler kafkaProducerHandler)
        {
            this.handler = kafkaProducerHandler;
            return this;
        }
    }
}

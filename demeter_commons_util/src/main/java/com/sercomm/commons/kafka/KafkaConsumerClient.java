package com.sercomm.commons.kafka;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;

public final class KafkaConsumerClient implements Closeable
{
    private Worker worker;
    private Thread workerThread;
    
    protected KafkaConsumerClient(Properties properties, List<String> subscriptions, KafkaConsumerHandler kafkaConsumerHandler)
    {
        this.worker = new Worker();
        this.worker.kafkaConsumerClient = this;
        this.worker.properties = properties;
        this.worker.subscriptions = subscriptions;
        this.worker.handler = kafkaConsumerHandler;      

        if(null != this.workerThread)
        {
            throw new IllegalStateException();
        }
        
        this.worker.alive = true;
        this.workerThread = new Thread(this.worker);
        this.workerThread.start();
    }

    @Override
    public void close() throws IOException
    {
        this.worker.alive = false;
        if(null != this.workerThread)
        {
            this.workerThread = null;
        }
    }
    

    private static class Worker implements Runnable
    {
        private KafkaConsumerClient kafkaConsumerClient;
        private Properties properties;
        private KafkaConsumer<byte[], byte[]> consumer;
        private KafkaConsumerHandler handler;
        private List<String> subscriptions;
        private boolean alive = true;
        
        private ConsumerRebalanceListener listener = new ConsumerRebalanceListener() 
        {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) 
            {
                if(null == handler)
                {
                    return;
                }
                
                for(TopicPartition topicPartition : partitions)
                {
                    handler.ConsumerPartitionRevoked(topicPartition.topic(), topicPartition.partition());
                }
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) 
            {
                if(null == handler)
                {
                    return;
                }
                
                for(TopicPartition topicPartition : partitions)
                {
                    handler.ConsumerPartitionAssigned(topicPartition.topic(), topicPartition.partition());
                }
            }
        };
        
        @Override
        public void run()
        {
            this.consumer = new KafkaConsumer<byte[], byte[]>(this.properties); 

            this.consumer.subscribe(this.subscriptions, this.listener);
            
            while(true == this.alive)
            {
                try
                {
                    //ConsumerRecords<String, String> records = this.consumer.poll(Duration.ofMillis(100L));
                    ConsumerRecords<byte[], byte[]> records = this.consumer.poll(100L);
                    if(null != this.handler)
                    {
                        for (ConsumerRecord<byte[], byte[]> record : records)
                        {                            
                            try
                            {                                
                                Map<String, byte[]> headers = new HashMap<>();
                                for(Header header : record.headers())
                                {
                                    headers.put(header.key(), header.value());
                                }

                                // trigger ConsumerMessageReceived
                                this.handler.ConsumerMessageReceived(
                                    this.kafkaConsumerClient,
                                    record.topic(), 
                                    record.partition(), 
                                    record.offset(), 
                                    headers,
                                    record.value());
                            }
                            catch(Throwable ignored) 
                            {
                                // 1. if exception occurred when parsing datagram which means invalid JSON string received, then skipping & dropping it
                                // 2. exception from callback method should be ignored
                            }
                        }
                    }
                }
                catch(Throwable t)
                {
                    // trigger ConsumerErrorOccurred
                    if(null != this.handler)
                    {
                        this.handler.ConsumerErrorOccurred(kafkaConsumerClient, t);
                    }
                }
            }
            
            if(null != this.consumer)
            {
                this.consumer.close();
                //this.consumer.close(Duration.ofMillis(1000L));
            }

            // trigger ConsumerClosed
            if(null != this.handler)
            {
                this.handler.ConsumerClosed(this.kafkaConsumerClient);
            }
        }        
    }
    
    public static class Builder
    {
        private Properties properties;
        private KafkaConsumerHandler handler;
        private List<String> subscriptions = new ArrayList<String>();

        public Builder()
        {
            this.properties = new Properties();
            
            this.properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.TRUE.toString().toLowerCase());
            this.properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "6000");
            this.properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.toString().toLowerCase());
            this.properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            this.properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");    
        }

        public KafkaConsumerClient build()
        {
            KafkaConsumerClient object = new KafkaConsumerClient(this.properties, this.subscriptions, this.handler);
            return object;
        }
        
        public Builder bootstrapServer(String address)
        {
            this.properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, address);
            return this;
        }
        
        public Builder group(String groupId)
        {
            this.properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            return this;
        }
        
        public Builder autoCommit(boolean enable)
        {
            this.properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(enable).toLowerCase());
            if(true == enable)
            {
                this.properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
            }
            
            return this;
        }
        
        public Builder subscribe(String topic)
        {
            if(false == this.subscriptions.contains(topic))
            {
                this.subscriptions.add(topic);
            }
            
            return this;
        }
        
        public Builder handler(KafkaConsumerHandler kafkaConsumerHandler)
        {
            this.handler = kafkaConsumerHandler;
            return this;
        }
    }
}

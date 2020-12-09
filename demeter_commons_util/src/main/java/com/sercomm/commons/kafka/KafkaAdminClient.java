package com.sercomm.commons.kafka;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;

public class KafkaAdminClient implements Closeable
{
    private org.apache.kafka.clients.admin.AdminClient adminClient;
    private int timeout = 5 * 1000;
    
    private KafkaAdminClient(Properties properties)
    {
        this.adminClient = org.apache.kafka.clients.admin.KafkaAdminClient.create(properties);
    }

    @Override
    public void close() throws IOException
    {
        if(null != this.adminClient)
        {
            //this.adminClient.close(Duration.ofMillis(1000L));
            this.adminClient.close();
        }
    }

    public void createTopic(String topic, int partitionCount)
    {
        if(null == this.adminClient)
        {
            throw new IllegalStateException();
        }
        
        try
        {
            KafkaFuture<Void> future = this.adminClient.createTopics(Collections.singleton(
                new NewTopic(topic, partitionCount, (short)1)),
                new CreateTopicsOptions().timeoutMs(timeout)).all();

            future.get();
        }
        catch(InterruptedException | ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public void createTopicIfNotExists(String topic, int partitionCount)
    {
        if(null == this.adminClient)
        {
            throw new IllegalStateException();
        }
        
        boolean contains = false;
        List<String> topics = this.listTopics();
        for(String candidate : topics)
        {
            if(0 == topic.compareTo(candidate))
            {
                contains = true;
                break;
            }
        }
        
        if(false == contains)
        {
            this.createTopic(topic, partitionCount);
        }
    }
    
    public List<String> listTopics()
    {
        if(null == this.adminClient)
        {
            throw new IllegalStateException();
        }
        
        ListTopicsOptions options = new ListTopicsOptions();
        options.timeoutMs(timeout);
        options.listInternal(false);
        
        try
        {
            ListTopicsResult result = this.adminClient.listTopics(options);
            
            Set<String> topics = result.names().get();
            
            return new ArrayList<String>(topics);
        }
        catch(InterruptedException | ExecutionException e)
        {
            throw new RuntimeException(e);
        }        
    }
    
    public Map<String, TopicDescription> describeTopic(String topic)
    {
        if(null == this.adminClient)
        {
            throw new IllegalStateException();
        }
        
        try
        {
            DescribeTopicsResult result = this.adminClient.describeTopics(Collections.singleton(topic));
            Map<String, TopicDescription> descriptions = result.all().get();
            return descriptions;
        }
        catch(InterruptedException | ExecutionException e)
        {
            throw new RuntimeException(e);
        }        
    }
    
    public void deleteTopic(String topic)
    {
        if(null == this.adminClient)
        {
            throw new IllegalStateException();
        }
        
        boolean contains = false;
        List<String> topics = this.listTopics();
        for(String candidate : topics)
        {
            if(0 == topic.compareTo(candidate))
            {
                contains = true;
                break;
            }
        }
        
        if(true == contains)
        {
            try
            {
                DeleteTopicsResult result = this.adminClient.deleteTopics(Collections.singleton(topic));
                result.all().get();
            }
            catch(InterruptedException | ExecutionException e)
            {
                throw new RuntimeException(e);
            }        
        }
    }
    
    public static class Builder
    {
        private Properties properties;
        
        public Builder()
        {
            this.properties = new Properties();
            this.properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        }
        
        public KafkaAdminClient build()
        {
            KafkaAdminClient object = new KafkaAdminClient(this.properties);
            return object;
        }
        
        public Builder bootstrapServer(String address)
        {
            this.properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, address);
            return this;
        }
    }
}

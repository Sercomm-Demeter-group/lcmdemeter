package com.sercomm.openfire.plugin.prop;

public class KafkaConfig
{
    private Boolean enable;
    private String bootstrapServers;
    private String topicName;
    private Integer topicPartitionCount;
    
    public KafkaConfig()
    {
    }
    
    public Boolean getEnable()
    {
        return this.enable;
    }
    
    public void setEnable(Boolean enable)
    {
        this.enable = enable;
    }
    
    public String getBootstrapServers()
    {
        return this.bootstrapServers;
    }
    
    public void setBootstrapServers(String bootstrapServers)
    {
        this.bootstrapServers = bootstrapServers;
    }
    
    public String getTopicName()
    {
        return this.topicName;
    }
    
    public void setTopicName(String topicName)
    {
        this.topicName = topicName;
    }
    
    public Integer getTopicPartitionCount()
    {
        return this.topicPartitionCount;
    }
    
    public void setTopicPartitionCount(int topicPartitionCount)
    {
        this.topicPartitionCount = topicPartitionCount;
    }
}

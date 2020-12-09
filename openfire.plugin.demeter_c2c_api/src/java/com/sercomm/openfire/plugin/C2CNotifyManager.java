package com.sercomm.openfire.plugin;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.kafka.KafkaAdminClient;
import com.sercomm.commons.kafka.KafkaProducerClient;
import com.sercomm.commons.kafka.KafkaProducerHandler;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.define.DeviceState;
import com.sercomm.openfire.plugin.dispatcher.DeviceStateDispatcher;
import com.sercomm.openfire.plugin.dispatcher.DeviceStateDispatcher.StateListener;

public class C2CNotifyManager extends ManagerBase implements KafkaProducerHandler
{
    private KafkaProducerClient producer;

    public static class Message
    {
        private String action;
        private Object data;
        
        public Message()
        {
        }
        
        public String getAction()
        {
            return this.action;
        }
        
        public void setAction(String action)
        {
            this.action = action;
        }
        
        public Object getData()
        {
            return this.data;
        }
        
        public void setData(Object data)
        {
            this.data = data;
        }
    }
    
    private C2CNotifyManager() 
    {
    }

    private static final class C2CNotifyManagerContainer
    {
        private static final C2CNotifyManager instance = new C2CNotifyManager();
    }

    public static C2CNotifyManager getInstance()
    {
        return C2CNotifyManagerContainer.instance;
    }

    public KafkaProducerClient getProducer()
    {
        return this.producer;
    }

    @Override
    protected void onInitialize()
    {
        try
        {
            DeviceStateDispatcher.addListener(this.deviceStateListener);
            
            if(true == PropertyManager.getInstance().getKafkaConfig().getEnable())
            {
                // self-create the required topics
                KafkaAdminClient.Builder adminBuilder = new KafkaAdminClient.Builder();
                try(KafkaAdminClient adminClient = adminBuilder.bootstrapServer(
                        PropertyManager.getInstance().getKafkaConfig().getBootstrapServers()).build())
                {
                    String topic = PropertyManager.getInstance().getKafkaConfig().getTopicName();
                    
                    adminClient.createTopicIfNotExists(
                        topic, 
                        PropertyManager.getInstance().getKafkaConfig().getTopicPartitionCount());
                }

                // create Kafka producer
                this.producer = new KafkaProducerClient.Builder()
                        .bootstrapServer(PropertyManager.getInstance().getKafkaConfig().getBootstrapServers())
                        .handler(this)
                        .build();
            }            
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
            throw new RuntimeException(t);
        }
    }

    @Override
    protected void onUninitialize()
    {
        try
        {
            DeviceStateDispatcher.removeListener(this.deviceStateListener);

            if(null != this.producer)
            {
                this.producer.close();
                this.producer = null;
            }            
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
            throw new RuntimeException(t);
        }
    }

    @Override
    public void ProducerMessageDelivering(
            KafkaProducerClient kafkaProducerClient, 
            String topic, 
            byte[] message)
    {
    }

    @Override
    public void ProducerMessageDeliveredResult(
            KafkaProducerClient kafkaProducerClient, 
            Exception e, 
            String topic, 
            int partition, 
            long offset, 
            byte[] message)
    {
        Log.write().debug("({},{},{}); {}", 
            topic, 
            partition,
            offset,
            null != e ? "errors: " + e.getMessage() : XStringUtil.BLANK);
    }

    @Override
    public void ProducerErrorOccurred(
            KafkaProducerClient kafkaProducerClient, 
            Throwable t)
    {
        // Log error
        Log.write().error(t.getMessage(), t);
    }

    @Override
    public void ProducerClosed(
            KafkaProducerClient kafkaProducerClient)
    {
        Log.write().warn("KAFKA PRODUCER WORKER EXIT");
    }

    private StateListener deviceStateListener = new StateListener()
    {
        @Override
        public void stateChanged(
                String serial, 
                String mac, 
                DeviceState oldState, 
                DeviceState newState, 
                long triggerTime)
        {
            if(false == PropertyManager.getInstance().getKafkaConfig().getEnable())
            {
                return;
            }
            
            Message message = null;
            try
            {    
                // produce 'online' message
                if(0 == oldState.compareTo(DeviceState.OFFLINE) &&
                   0 == newState.compareTo(DeviceState.ONLINE))
                {
                    DeviceCache deviceCache = 
                            DeviceManager.getInstance().getDeviceCache(serial, mac);
                    
                    HashMap<String, Object> data = new HashMap<>();
                    data.put("serial", serial);
                    data.put("mac", mac);
                    data.put("model", deviceCache.getModelName());

                    message = new Message();
                    message.setAction("online");
                    message.setData(data);                        
                }
                
                // produce 'offline' message
                if(0 == oldState.compareTo(DeviceState.ONLINE) &&
                   0 == newState.compareTo(DeviceState.OFFLINE))
                {
                    DeviceCache deviceCache = 
                            DeviceManager.getInstance().getDeviceCache(serial, mac);
                    
                    HashMap<String, Object> data = new HashMap<>();
                    data.put("serial", serial);
                    data.put("mac", mac);
                    data.put("model", deviceCache.getModelName());

                    message = new Message();
                    message.setAction("offline");
                    message.setData(data);                        
                }

                if(null != message)
                {
                    KafkaProducerClient producer = C2CNotifyManager.getInstance().getProducer();
                    if(null != producer)
                    {
                        producer.deliver(
                            PropertyManager.getInstance().getKafkaConfig().getTopicName(),
                            null, 
                            Json.build(message).getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            catch(Throwable t)
            {
                Log.write().error(t.getMessage(), t);
            }
        }
    };
}

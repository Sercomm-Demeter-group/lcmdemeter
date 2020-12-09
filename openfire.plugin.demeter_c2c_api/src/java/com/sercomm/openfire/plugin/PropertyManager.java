package com.sercomm.openfire.plugin;

import java.util.Map;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.Log;
import com.sercomm.openfire.plugin.prop.C2CPropertyEnum;
import com.sercomm.openfire.plugin.prop.KafkaConfig;

public class PropertyManager extends ManagerBase implements PropertyEventListener
{
    private KafkaConfig kafkaConfig;

    private static final KafkaConfig DEFAULT_NOTIFY_KAFKA_CONFIG = new KafkaConfig();
    static {
        // clean unnecessary properties
        for(String propertyName : JiveGlobals.getPropertyNames())
        {
            if(propertyName.startsWith("sercomm.c2c"))
            {
                if(null != C2CPropertyEnum.fromString(propertyName))
                {
                    continue;
                }
                
                boolean found = false;
                for(C2CPropertyEnum value : C2CPropertyEnum.values())
                {
                    if(propertyName.matches(value.toString() + ".[0-9]*"))
                    {
                        found = true;
                        break;
                    }
                }
                
                if(false == found)
                {
                    JiveGlobals.deleteProperty(propertyName);
                }
            }
        }

        // set default values
        DEFAULT_NOTIFY_KAFKA_CONFIG.setEnable(false);
        DEFAULT_NOTIFY_KAFKA_CONFIG.setBootstrapServers("127.0.0.1:9092");
        DEFAULT_NOTIFY_KAFKA_CONFIG.setTopicName("sercomm.demeter.c2c.notification");
        DEFAULT_NOTIFY_KAFKA_CONFIG.setTopicPartitionCount(4);
    }

    private static final class PropertyManagerContainer
    {
        private static final PropertyManager instance = new PropertyManager();
    }

    private PropertyManager()
    {
    }
    
    public static PropertyManager getInstance()
    {
        return PropertyManagerContainer.instance;
    }

    @Override
    protected void onInitialize()
    {
        try
        {
            PropertyEventDispatcher.addListener(this);

            // load required properties
            this.load();
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
        }
        finally
        {
            // save anyway
            try
            {
                this.save();
            }
            catch(Throwable ignored) {}
        }
    }

    @Override
    protected void onUninitialize()
    {
        // TODO Auto-generated method stub
        
    }

    private void load() throws Throwable
    {
        // load raw text
        String kafkaConfigString = 
                JiveGlobals.getProperty(C2CPropertyEnum.C2C_NOTIFY_KAFKA_CONFIG.toString(), Json.build(DEFAULT_NOTIFY_KAFKA_CONFIG));
        
        // assign to objects
        this.kafkaConfig = 
                Json.mapper().readValue(kafkaConfigString, KafkaConfig.class);
    }
    
    private void save() throws Throwable
    {
        JiveGlobals.setProperty(
            C2CPropertyEnum.C2C_NOTIFY_KAFKA_CONFIG.toString(), Json.build(this.kafkaConfig));
    }

    @Override
    public void propertySet(String property, Map<String, Object> params)
    {
        if(property.startsWith("sercomm.c2c"))
        {
            try
            {
                // load new values
                this.load();

                switch(C2CPropertyEnum.fromString(property))
                {
                    case C2C_NOTIFY_KAFKA_CONFIG:
                    {                        
                        // re-initialize
                        C2CNotifyManager.getInstance().uninitialize();
                        C2CNotifyManager.getInstance().initialize();
                        break;
                    }
                    default:
                        break;
                }
            }
            catch(Throwable t)
            {
                Log.write().error(t.getMessage(), t);
            }
        }
    }

    @Override
    public void propertyDeleted(String property, Map<String, Object> params)
    {
        if(property.startsWith("sercomm.c2c"))
        {
            try
            {
                // load new values
                this.load();
                
                switch(C2CPropertyEnum.fromString(property))
                {
                    case C2C_NOTIFY_KAFKA_CONFIG:
                    {                        
                        // re-initialize
                        C2CNotifyManager.getInstance().uninitialize();
                        C2CNotifyManager.getInstance().initialize();
                        break;
                    }
                    default:
                        break;
                }
            }
            catch(Throwable t)
            {
                Log.write().error(t.getMessage(), t);
            }
        }
    }

    @Override
    public void xmlPropertySet(String property, Map<String, Object> params)
    {
    }

    @Override
    public void xmlPropertyDeleted(String property, Map<String, Object> params)
    {
    }

    public KafkaConfig getKafkaConfig()
    {
        return this.kafkaConfig;
    }
}

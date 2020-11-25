package com.sercomm.openfire.plugin.dispatcher;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sercomm.openfire.plugin.define.DeviceState;

public class DeviceStateDispatcher
{
    private final static Queue<StateListener> listeners =
            new ConcurrentLinkedQueue<StateListener>();

    public interface StateListener
    {
        void stateChanged(
                String serial, 
                String mac,
                DeviceState oldState,
                DeviceState newState,
                long triggerTime);
    }

    public static void addListener(StateListener stateListener)
    {
        listeners.add(stateListener);
    }
    
    public static void removeListener(StateListener stateListener)
    {
        listeners.remove(stateListener);
    }

    public static void dispatchStateChanged(
            String serial,
            String mac,
            DeviceState oldState,
            DeviceState newState)
    {
        Iterator<StateListener> iterator = listeners.iterator();
        while(iterator.hasNext())
        {
            StateListener listener = iterator.next();
            try
            {
                listener.stateChanged(
                    serial, 
                    mac, 
                    oldState, 
                    newState, 
                    System.currentTimeMillis());
            }
            catch(Throwable ignored) {}
        }
    }
}

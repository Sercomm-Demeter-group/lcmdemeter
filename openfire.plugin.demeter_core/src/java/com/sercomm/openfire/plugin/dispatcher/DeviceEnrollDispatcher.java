package com.sercomm.openfire.plugin.dispatcher;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DeviceEnrollDispatcher
{
    private final static Queue<EnrollListener> listeners =
            new ConcurrentLinkedQueue<EnrollListener>();

    public interface EnrollListener
    {
        void enrolled(
                String serial,
                String mac,
                String modelName,
                long triggerTime);
        void deleted(
                String serial,
                String mac,
                String modelName,
                long triggerTime);
    }

    public static void addListener(EnrollListener enrollListener)
    {
        listeners.add(enrollListener);
    }

    public static void removeListener(EnrollListener enrollListener)
    {
        listeners.remove(enrollListener);
    }

    public static void dispatchEnrolled(String serial, String mac, String modelName)
    {
        Iterator<EnrollListener> iterator = listeners.iterator();
        while(iterator.hasNext())
        {
            EnrollListener listener = iterator.next();
            try
            {
                listener.enrolled(serial, mac, modelName, System.currentTimeMillis());
            }
            catch(Throwable ignored) {}
        }
    }

    public static void dispatchDeleted(String serial, String mac, String modelName)
    {
        Iterator<EnrollListener> iterator = listeners.iterator();
        while(iterator.hasNext())
        {
            EnrollListener listener = iterator.next();
            try
            {
                listener.deleted(serial, mac, modelName, System.currentTimeMillis());
            }
            catch(Throwable ignored) {}
        }
    }
}

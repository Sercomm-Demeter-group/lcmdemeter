package com.sercomm.openfire.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.TaskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.data.frontend.AppEventRecord;
import com.sercomm.openfire.plugin.define.AppEventType;
import com.sercomm.openfire.plugin.util.DbConnectionUtil;

public class AppEventManager extends ManagerBase
{
    private static final Logger log = LoggerFactory.getLogger(AppEventManager.class);

    private final static int _BATCH_COUNT = 100;
    private final Queue<AppEventRecord> queue = new ConcurrentLinkedQueue<AppEventRecord>();
    private final TimerTask writeTask;
    
    private final static String TABLE_S_APP_EVENT_RECORD = "sAppEventRecord";
    private final static String SQL_INSERT = String.format(
        "INSERT INTO `%s`(`appId`,`appName`,`serial`,`mac`,`userId`,`eventType`,`unixTime`,`partitionIdx`) " +
        "VALUES(?,?,?,?,?,?,?,?)",
        TABLE_S_APP_EVENT_RECORD);
    private final static String SQL_QUERY = String.format(
        "SELECT * FROM `%s` WHERE `serial`=? AND `mac`=?",
        TABLE_S_APP_EVENT_RECORD);
    private final static String SQL_DELETE = String.format(
        "DELETE FROM `%s` WHERE `serial`=? AND `mac`=?",
        TABLE_S_APP_EVENT_RECORD);
    
    private static class AppEventManagerContainer
    {
        private final static AppEventManager instance = new AppEventManager();
    }
    
    private AppEventManager()
    {
        this.writeTask = new WriteTask();
    }
    
    public static AppEventManager getInstance()
    {
        return AppEventManagerContainer.instance;
    }
    
    @Override
    protected void onInitialize()
    {
        if(true == this.isInitialized())
        {
            TaskEngine.getInstance().cancelScheduledTask(this.writeTask);
        }
        
        TaskEngine.getInstance().scheduleAtFixedRate(
            this.writeTask, 
            10 * 1000L, 
            10 * 1000L);
    }

    @Override
    protected void onUninitialize()
    {
        if(true == this.isInitialized())
        {
            TaskEngine.getInstance().cancelScheduledTask(this.writeTask);            
        }
    }

    private class WriteTask extends TimerTask
    {        
        @Override
        public void run()
        {
            boolean abortTransaction = true;
            int insertCount = 0;
            String errorMessage = XStringUtil.BLANK;

            final List<AppEventRecord> records = new ArrayList<AppEventRecord>();
                        
            Connection conn = null;
            PreparedStatement stmt = null;
            try
            {
                do
                {
                    AppEventRecord record = null;
                    do
                    {
                        record = queue.poll();
                        if(null == record)
                        {
                            break;
                        }
                        
                        int retryCount = record.getRetryCount();
                        if(retryCount > 10) // retry 10 times
                        {
                            // drop it
                            continue;
                        }
                        
                        records.add(record);
                    }
                    while(true);

                    if(0 == records.size()) break;

                    conn = DbConnectionManager.getConnection();
                    conn = DbConnectionUtil.openTransaction(conn);
                    
                    Iterator<AppEventRecord> iterator = records.iterator();
                    stmt = conn.prepareStatement(SQL_INSERT);
                    while(iterator.hasNext())
                    {
                        record = iterator.next();
                        
                        int idx = 0;
                        stmt.setString(++idx, record.getAppId());
                        stmt.setString(++idx, record.getAppName());
                        stmt.setString(++idx, record.getSerial());
                        stmt.setString(++idx, record.getMac());
                        stmt.setString(++idx, record.getUserId());
                        stmt.setString(++idx, record.getEventType());
                        stmt.setLong(++idx, record.getUnixTime());
                        stmt.setString(++idx, record.getPartitionIdx());
                        
                        stmt.addBatch();
                        
                        if(++insertCount % _BATCH_COUNT == 0)
                        {
                            stmt.executeBatch();
                        }
                    }                    
                    stmt.executeBatch();

                    abortTransaction = false;                    
                }
                while(false);
            }
            catch(Throwable t)
            {
                errorMessage = t.getMessage();
                log.error(t.getMessage(), t);
                
                // increase the retry count
                for(AppEventRecord record : records)
                {
                    record.setRetryCount(record.getRetryCount() + 1);
                }
                
                // put the reserved record(s) back
                queue.addAll(records);
                
                // reset the inserted record count
                insertCount = 0;
            }
            finally
            {
                DbConnectionManager.closeStatement(stmt);
                DbConnectionUtil.closeTransaction(conn, abortTransaction);
                DbConnectionManager.closeConnection(conn);
            }
            
            log.debug("({},{})={}",
                insertCount,
                queue.size(),
                errorMessage);
        }
    }
    
    public void triggerAppEvent(
            String appId,
            String appName,
            String serial,
            String mac,
            String userId,
            AppEventType appEventType)
    {
        final DateTime dateTime = DateTime.now();
        dateTime.setTimeZone(TimeZone.getTimeZone("UTC"));

        // TODO: trigger the event to event listeners
        
        // leave a record to database        
        AppEventRecord object = new AppEventRecord();
        object.setAppId(appId);
        object.setAppName(appName);
        object.setSerial(serial);
        object.setMac(mac);
        object.setUserId(userId);
        object.setEventType(appEventType.toString());
        object.setUnixTime(dateTime.getTimeInMillis());
        object.setPartitionIdx(dateTime.toString(DateTime.FORMAT_MYSQL));
        
        this.queue.offer(object);
    }
    
    public List<AppEventRecord> getAppEventRecords(
            String serial,
            String mac)
    throws Throwable
    {
        List<AppEventRecord> records = new ArrayList<AppEventRecord>();        

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY);
            
            int idx = 0;
            stmt.setString(++idx, serial);
            stmt.setString(++idx, mac);
            
            rs = stmt.executeQuery();
            while(rs.next())
            {
                AppEventRecord object = AppEventRecord.from(rs);
                records.add(object);
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return records;
    }
    
    public void deleteAppEventRecords(
            String serial,
            String mac)
    throws Throwable
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_DELETE);
            
            int idx = 0;
            stmt.setString(++idx, serial);
            stmt.setString(++idx, mac);
            
            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }        
    }
}

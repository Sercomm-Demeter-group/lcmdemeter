package com.sercomm.openfire.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.TaskEngine;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.util.Log;
import com.sercomm.openfire.plugin.data.frontend.SystemRecord;
import com.sercomm.openfire.plugin.util.DbConnectionUtil;

public class SystemRecordManager extends ManagerBase
{
    private final static int _BATCH_COUNT = 100;
    private final Queue<SystemRecord> queue = new ConcurrentLinkedQueue<SystemRecord>();    
    private final TimerTask writeTask;

    private static class SystemRecordManagerContainer
    {
        private final static SystemRecordManager instance = new SystemRecordManager();
    }
    
    private SystemRecordManager()
    {
        this.writeTask = new WriteTask();
    }
    
    public static SystemRecordManager getInstance()
    {
        return SystemRecordManagerContainer.instance;
    }

    @Override
    protected void onInitialize()
    {
        if(true == this.isInitialized())
        {
            TaskEngine.getInstance().cancelScheduledTask(this.writeTask);
        }

        // perform local task only
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
        private final static String TABLE_S_SYSTEM_RECORD = "sSystemRecord";
        private final String SQL_INSERT = String.format(
            "INSERT INTO `%s`(`platform`,`serial`,`mac`,`category`,`type`,`unixTime`,`detail`) VALUES(?,?,?,?,?,?,?)",
            TABLE_S_SYSTEM_RECORD);

        @Override
        public void run()
        {
            boolean abort = false;
            int insertCount = 0;

            Connection conn = null;
            PreparedStatement stmt = null;
            final List<SystemRecord> records = new ArrayList<SystemRecord>();
            try
            {
                SystemRecord record = null;
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
                
                if(0 == records.size()) return;
                
                conn = DbConnectionManager.getConnection();
                conn = DbConnectionUtil.openTransaction(conn);
                
                Iterator<SystemRecord> iterator = records.iterator();
                stmt = conn.prepareStatement(SQL_INSERT);
                while(iterator.hasNext())
                {
                    record = iterator.next();
                    
                    int idx = 0;
                    stmt.setString(++idx, record.getPlatform());
                    stmt.setString(++idx, record.getSerial());
                    stmt.setString(++idx, record.getMac());
                    stmt.setInt(++idx, record.getCategory());
                    stmt.setString(++idx, record.getType());
                    stmt.setLong(++idx, record.getUnixTime());
                    stmt.setString(++idx, record.getDetail());
                    
                    stmt.addBatch();
                    
                    if(++insertCount % _BATCH_COUNT == 0)
                    {
                        stmt.executeBatch();
                    }
                }

                stmt.executeBatch();
            }
            catch(Throwable t)
            {
                abort = true;
                Log.write().error(t.getMessage(), t);
                
                // increase the retry count
                for(SystemRecord record : records)
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
                DbConnectionUtil.closeTransaction(conn, abort);
                DbConnectionManager.closeConnection(conn);
            }

            Log.write().debug("({},{});",
                insertCount,
                queue.size());                
        }
    }
}

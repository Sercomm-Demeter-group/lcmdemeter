package com.sercomm.openfire.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.data.frontend.Batch;
import com.sercomm.openfire.plugin.data.frontend.BatchData;
import com.sercomm.openfire.plugin.define.BatchCommand;
import com.sercomm.openfire.plugin.define.BatchState;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.task.BatchTask;
import com.sercomm.openfire.plugin.util.DbConnectionUtil;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.cluster.ClusterNodeInfo;
import org.jivesoftware.util.cache.CacheFactory;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class BatchManager extends ManagerBase
{
    // private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);

    private static final String TABLE_S_BATCH = "sBatch";
    private static final String SQL_QUERY_BATCH = String.format(
        "SELECT * FROM `%s` WHERE `id`=?",
        TABLE_S_BATCH);
    private static final String SQL_UPDATE_BATCH = String.format(
        "INSERT INTO `%s`(`id`,`appId`,`versionId`,`command`,`state`,`nodeId`,`creationTime`,`updatedTime`,`totalCount`,`data`) " +
        "VALUES(?,?,?,?,?,?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE `state`=?,`nodeId`=?,`updatedTime`=?,`doneCount`=?,`failedCount`=?,`data`=?",
        TABLE_S_BATCH);

    private BatchManager()
    {
    }

    private static class BatchManagerContainer
    {
        private final static BatchManager instance = new BatchManager();
    }

    public static BatchManager getInstance()
    {
        return BatchManagerContainer.instance;
    }

    @Override
    protected void onInitialize() 
    {
    }

    @Override
    protected void onUninitialize() 
    {
    }

    public Batch getBatch(
        String batchId)
    throws DemeterException, Throwable
    {
        Batch batch = null;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;        
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_BATCH);

            int idx = 0;
            stmt.setString(++idx, batchId);

            rs = stmt.executeQuery();
            if(!rs.next())
            {
                throw new DemeterException("BATCH CANNOT BE FOUND");
            }

            batch = Batch.from(rs);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        return batch;
    }

    public Batch updateBatch(
        String batchId,
        String applicationId,
        String versionId,
        BatchCommand batchCommand,
        BatchState batchState,
        List<String> totalDevices,
        List<String> doneDevices,
        List<String> failedDevices)
    throws DemeterException, Throwable
    {
        Batch batch = null;
        DateTime now = DateTime.now();

        boolean isNewTask = false;
        if(XStringUtil.isBlank(batchId))
        {
            batchId = UUID.randomUUID().toString();
            batch = new Batch();
            batch.setId(batchId);
            batch.setApplicationId(applicationId);
            batch.setVersionId(versionId);
            batch.setCommand(batchCommand.intValue());
            batch.setNodeId(XMPPServer.getInstance().getNodeID().toString());
            batch.setCreationTime(now.getTimeInMillis());
            batch.setUpdatedTime(now.getTimeInMillis());

            isNewTask = true;
        }
        else
        {
            batch = this.getBatch(batchId);
            batch.setNodeId(XMPPServer.getInstance().getNodeID().toString());
            batch.setUpdatedTime(now.getTimeInMillis());
        }

        if(null == batchState)
        {
            batchState = BatchState.PENDING;
        }
        batch.setState(batchState.toString());

        // check if the application ID is still valid
        AppManager.getInstance().getApp(batch.getApplicationId());
        // check if the version ID is still valid
        AppManager.getInstance().getAppVersion(batch.getVersionId());

        batch.setTotalCount(totalDevices.size());
        batch.setDoneCount(doneDevices.size());
        batch.setFailedCount(failedDevices.size());

        BatchData batchData = new BatchData(
            totalDevices,
            doneDevices,
            failedDevices);
        batch.setData(batchData.toByteArray());

        boolean abortTransaction = true;
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            DbConnectionUtil.openTransaction(conn);
            stmt = conn.prepareStatement(SQL_UPDATE_BATCH);

            int idx = 0;
            // `id`,`appId`,`versionId`,`command`,`state`,`nodeId`,`creationTime`,`updatedTime`,`totalCount`,`data`
            // `state`=?,`nodeId`=?,`updatedTime`=?,`doneCount`=?,`failedCount`=?,`data`=?
            stmt.setString(++idx, batch.getId());
            stmt.setString(++idx, batch.getApplicationId());
            stmt.setString(++idx, batch.getVersionId());
            stmt.setInt(++idx, batch.getCommand());
            stmt.setString(++idx, batch.getState());
            stmt.setString(++idx, batch.getNodeId());
            stmt.setLong(++idx, batch.getCreationTime());
            stmt.setLong(++idx, batch.getUpdatedTime());
            stmt.setInt(++idx, batch.getTotalCount());
            stmt.setBytes(++idx, batchData.toByteArray());

            stmt.setString(++idx, batch.getState());
            stmt.setString(++idx, batch.getNodeId());
            stmt.setLong(++idx, batch.getUpdatedTime());
            stmt.setInt(++idx, batch.getDoneCount());
            stmt.setInt(++idx, batch.getFailedCount());
            stmt.setBytes(++idx, batch.getData());
            stmt.executeUpdate();

            // get cluster nodes information
            List<ClusterNodeInfo> nodes = new ArrayList<>(ClusterManager.getNodesInfo());
            if(nodes.isEmpty())
            {
                throw new DemeterException("NO CLUSTER NODE AVAILABLE");
            }

            // random a cluster node to execute the batch task
            Collections.shuffle(nodes);
            ClusterNodeInfo node = nodes.get(0);

            // create cluster task if necessary
            if(true == isNewTask)
            {
                CacheFactory.doClusterTask(
                    new BatchTask(batchId), 
                    node.getNodeID().toByteArray());
            }

            abortTransaction = false;
        }
        finally
        {
            DbConnectionManager.closeStatement(stmt);
            DbConnectionUtil.closeTransaction(conn, abortTransaction);
            DbConnectionManager.closeConnection(conn);
        }

        return batch;
    }
}

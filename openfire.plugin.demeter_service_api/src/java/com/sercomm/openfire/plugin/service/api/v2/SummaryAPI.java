package com.sercomm.openfire.plugin.service.api.v2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.define.EndUserRole;

@Path(SummaryAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR, EndUserRole.OPERATOR})
public class SummaryAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(SummaryAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;

    @GET
    @Path("summary")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get()
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
            int totalDevicesCount = 0;
            int onlineDevicesCount = 0;
            int totalApplicationsCount = 0;
            int executingBatchesCount = 0;
    
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            // query total devices count
            try
            {
                StringBuilder builder = new StringBuilder();
                builder.append("SELECT COUNT(*) AS `count` FROM (");
                builder.append("SELECT `serial`,`mac` FROM `sDeviceProp` ");
                builder.append("GROUP BY `serial`,`mac` HAVING COUNT(`serial`) > 0 AND COUNT(`mac`) > 0) AS `t`");

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(builder.toString());
                rs = stmt.executeQuery();

                if(rs.next())
                {
                    totalDevicesCount = rs.getInt("count");
                }
                else
                {
                    throw new InternalErrorException("FAILED TO FETCH TOTAL DEVICES COUNT");
                }
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }

            // query online devices count
            try
            {
                StringBuilder builder = new StringBuilder();
                builder.append("SELECT COUNT(*) AS `count` FROM (");
                builder.append("SELECT `serial`,`mac` FROM `sDeviceProp` ");
                builder.append("WHERE `name`='sercomm.device.state' AND `propValue` LIKE 'ONLINE') AS `t`");

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(builder.toString());
                rs = stmt.executeQuery();

                if(rs.next())
                {
                    onlineDevicesCount = rs.getInt("count");
                }
                else
                {
                    throw new InternalErrorException("FAILED TO FETCH TOTAL ONLINE DEVICES COUNT");
                }
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }

            // query total applications count
            try
            {
                StringBuilder builder = new StringBuilder();
                builder.append("SELECT COUNT(*) AS `count` FROM `sApp`");

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(builder.toString());
                rs = stmt.executeQuery();

                if(rs.next())
                {
                    totalApplicationsCount = rs.getInt("count");
                }
                else
                {
                    throw new InternalErrorException("FAILED TO FETCH TOTAL APPLICATIONS COUNT");
                }
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }

            // query current executing batches count
            try
            {
                StringBuilder builder = new StringBuilder();
                builder.append("SELECT COUNT(*) AS `count` FROM `sBatch` ");
                builder.append("WHERE `state`='EXECUTING'");

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(builder.toString());
                rs = stmt.executeQuery();

                if(rs.next())
                {
                    executingBatchesCount = rs.getInt("count");
                }
                else
                {
                    throw new InternalErrorException("FAILED TO FETCH TOTAL EXECUTING BATCH COUNT");
                }
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }

            GetSummaryResult result = new GetSummaryResult();
            result.setTotalDevicesCount(totalDevicesCount);
            result.setOnlineDevicesCount(onlineDevicesCount);
            result.setTotalApplicationsCount(totalApplicationsCount);
            result.setExecutingBatchesCount(executingBatchesCount);

            // response
            BodyPayload bodyPayload = new BodyPayload()
                .withMeta(null)
                .withData(result);

            response = Response
                .status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(bodyPayload.toString())
                .build();            
        }
        catch(UMEiException e)
        {
            status = e.getErrorStatus();
            errorMessage = e.getMessage();
            throw e;
        }
        catch(Throwable t)
        {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            errorMessage = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);
            throw new InternalErrorException(t.getMessage());
        }

        log.info("({},{}); {}",
            userId,
            sessionId,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    public static class GetSummaryResult
    {
        private int totalDevicesCount;
        private int onlineDevicesCount;
        private int totalApplicationsCount;
        private int executingBatchesCount;

        public int getTotalDevicesCount()
        {
            return this.totalDevicesCount;
        }
        public void setTotalDevicesCount(int totalDevicesCount)
        {
            this.totalDevicesCount = totalDevicesCount;
        }
        public int getOnlineDevicesCount()
        {
            return this.onlineDevicesCount;
        }
        public void setOnlineDevicesCount(int onlineDevicesCount)
        {
            this.onlineDevicesCount = onlineDevicesCount;
        }
        public int getTotalApplicationsCount()
        {
            return this.totalApplicationsCount;
        }
        public void setTotalApplicationsCount(int totalApplicationsCount)
        {
            this.totalApplicationsCount = totalApplicationsCount;
        }
        public int getExecutingBatchesCount()
        {
            return this.executingBatchesCount;
        }
        public void setExecutingBatchesCount(int executingBatchesCount)
        {
            this.executingBatchesCount = executingBatchesCount;
        }
    }
}

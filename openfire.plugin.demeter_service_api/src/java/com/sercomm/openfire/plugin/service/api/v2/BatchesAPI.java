package com.sercomm.openfire.plugin.service.api.v2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.Meta;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.data.frontend.Batch;
import com.sercomm.openfire.plugin.data.frontend.BatchData;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(BatchAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR, EndUserRole.OPERATOR})
public class BatchesAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(BatchesAPI.class);
    
    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;
    
    @GET
    @Path("batches")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get(
            @QueryParam("from") Integer from,
            @QueryParam("size") Integer size,
            @QueryParam("begin") String begin,
            @QueryParam("end") String end,
            @QueryParam("filter") List<String> filters)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
            DateTime beginTime = null;
            DateTime endTime = null;

            if(XStringUtil.isNotBlank(begin))
            {
                try
                {
                    beginTime = DateTime.from(begin, DateTime.FORMAT_ISO);
                }
                catch(Throwable t1)
                {
                    status = Response.Status.BAD_REQUEST;
                    errorMessage = "INVALID 'begin': " + begin;
    
                    throw new UMEiException(
                        errorMessage,
                        Response.Status.BAD_REQUEST);
                }
            }

            if(XStringUtil.isNotBlank(end))
            {
                try
                {
                    endTime = DateTime.from(end, DateTime.FORMAT_ISO);
                }
                catch(Throwable t1)
                {
                    status = Response.Status.BAD_REQUEST;
                    errorMessage = "INVALID 'end': " + end;
    
                    throw new UMEiException(
                        errorMessage,
                        Response.Status.BAD_REQUEST);
                }
            }

            int totalCount = 0;

            // query total rows count
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                List<Object> arguments = new ArrayList<>();

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(
                    generateQueryTotalStatement(beginTime, endTime, filters, arguments));

                for(int idx = 0; idx < arguments.size(); idx++)
                {
                    stmt.setObject(idx + 1, arguments.get(idx));
                }

                rs = stmt.executeQuery();
                rs.next();

                totalCount = rs.getInt("count");
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }

            // query rows data
            List<GetBatchResult> result = new ArrayList<>();
            try
            {
                List<Object> arguments = new ArrayList<>();

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(
                    generateQueryRowsStatement(from, size, beginTime, endTime, filters, arguments));
                
                for(int idx = 0; idx < arguments.size(); idx++)
                {
                    stmt.setObject(idx + 1, arguments.get(idx));
                }

                rs = stmt.executeQuery();
                while(rs.next())
                {
                    Batch batch = Batch.from(rs);
                    BatchData batchData = new BatchData(batch.getData());

                    double value = (double) (batchData.getDoneDevices().size() + batchData.getFailedDevices().size()) / (double) batchData.getTotalDevices().size();

                    GetBatchResult object = new GetBatchResult();
                    object.setBatchId(batch.getId());
                    object.setCommand(batch.getCommand());
                    object.setStatus(batch.getState());
                    object.setCreationTime(DateTime.from(batch.getCreationTime()).toString(DateTime.FORMAT_ISO_MS));
                    object.setProgress((int)(value * 100));
                    object.setTotalDevicesCount(batchData.getTotalDevices().size());
                    object.setFailureDevicesCount(batchData.getFailedDevices().size());

                    result.add(object);
                }

                // response
                BodyPayload bodyPayload = new BodyPayload()
                        .withMeta(new Meta()
                            .withFrom(from == null ? 0 : from)
                            .withSize(result.size())
                            .withTotal(totalCount))
                        .withData(result);

                response = Response
                    .status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(bodyPayload.toString())
                    .build();            
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }
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

        log.info("({},{},{},{},{},{},{}); {}",
            userId,
            sessionId,
            from,
            size,
            begin,
            end,
            filters,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    private static String generateQueryTotalStatement(
        DateTime beginTime,
        DateTime endTime,
        List<String> filters,
        List<Object> arguments)
    throws UMEiException, InternalErrorException
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT COUNT(*) AS `count` FROM `sBatch` ");

        if((null != beginTime && null != endTime) || !filters.isEmpty())
        {
            builder.append("WHERE ");
        }

        if(null != beginTime && null != endTime)
        {
            builder.append("`creationTime` > ? AND `creationTime` < ? ");
            arguments.add(beginTime.getTimeInMillis());
            arguments.add(endTime.getTimeInMillis());

            if(!filters.isEmpty())
            {
                builder.append("AND ");
            }
        }

        List<String[]> statusFilters = new ArrayList<>();

        if(!filters.isEmpty())
        {
            // preprocess the filters
            Iterator<String> iterator = filters.iterator();
            while(iterator.hasNext())
            {
                final String filter = iterator.next();
                String[] tokens = filter.split(":", 2);
                if(2 != tokens.length)
                {
                    throw new UMEiException(
                        "INVALID FILTER: " + filter,
                        Response.Status.BAD_REQUEST);
                }

                switch(tokens[0])
                {
                    case "status":
                        statusFilters.add(tokens);
                        break;
                    default:
                        throw new UMEiException(
                            "INVALID FILTER ATTRIBUTE: " + tokens[0],
                            Response.Status.BAD_REQUEST);
                }
            }                
        }

        Iterator<String[]> iterator = statusFilters.iterator();
        while(iterator.hasNext())
        {
            final String[] tokens = iterator.next();

            builder.append("(`state` = ? ");
            arguments.add(tokens[1]);

            if(iterator.hasNext())
            {
                builder.append("OR ");
            }
            else
            {
                builder.append(") ");
            }
        }

        return builder.toString();
    }

    private static String generateQueryRowsStatement(
        Integer from,
        Integer size,
        DateTime beginTime,
        DateTime endTime,
        List<String> filters,
        List<Object> arguments)
    throws UMEiException, InternalErrorException
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT * FROM `sBatch` ");

        if((null != beginTime && null != endTime) || !filters.isEmpty())
        {
            builder.append("WHERE ");
        }

        if(null != beginTime && null != endTime)
        {
            builder.append("`creationTime` > ? AND `creationTime` < ? ");
            arguments.add(beginTime.getTimeInMillis());
            arguments.add(endTime.getTimeInMillis());

            if(!filters.isEmpty())
            {
                builder.append("AND ");
            }
        }

        List<String[]> statusFilters = new ArrayList<>();

        if(!filters.isEmpty())
        {
            // preprocess the filters
            Iterator<String> iterator = filters.iterator();
            while(iterator.hasNext())
            {
                final String filter = iterator.next();
                String[] tokens = filter.split(":", 2);
                if(2 != tokens.length)
                {
                    throw new UMEiException(
                        "INVALID FILTER: " + filter,
                        Response.Status.BAD_REQUEST);
                }

                switch(tokens[0])
                {
                    case "status":
                        statusFilters.add(tokens);
                        break;
                    default:
                        throw new UMEiException(
                            "INVALID FILTER ATTRIBUTE: " + tokens[0],
                            Response.Status.BAD_REQUEST);
                }
            }                
        }

        Iterator<String[]> iterator = statusFilters.iterator();
        while(iterator.hasNext())
        {
            final String[] tokens = iterator.next();

            builder.append("(`state` = ? ");
            arguments.add(tokens[1]);

            if(iterator.hasNext())
            {
                builder.append("OR ");
            }
            else
            {
                builder.append(") ");
            }
        }

        builder.append("ORDER BY `creationTime` DESC ");

        if(null != from && null != size)
        {
            builder.append("LIMIT ?,?");
            arguments.add(from);
            arguments.add(size);
        }

        return builder.toString();
    }

    public static class GetBatchResult
    {
        private String batchId;
        private String status;
        private String creationTime;
        private Integer command;
        private Integer progress;
        private Integer totalDevicesCount;
        private Integer failureDevicesCount;

        public String getBatchId()
        {
            return batchId;
        }
        public void setBatchId(String batchId)
        {
            this.batchId = batchId;
        }
        public String getStatus()
        {
            return status;
        }
        public void setStatus(String status)
        {
            this.status = status;
        }
        public String getCreationTime()
        {
            return creationTime;
        }
        public void setCreationTime(String creationTime)
        {
            this.creationTime = creationTime;
        }
        public Integer getCommand()
        {
            return command;
        }
        public void setCommand(Integer command)
        {
            this.command = command;
        }
        public Integer getProgress()
        {
            return progress;
        }
        public void setProgress(Integer progress)
        {
            this.progress = progress;
        }
        public Integer getTotalDevicesCount()
        {
            return totalDevicesCount;
        }
        public void setTotalDevicesCount(Integer totalDevicesCount)
        {
            this.totalDevicesCount = totalDevicesCount;
        }
        public Integer getFailureDevicesCount()
        {
            return failureDevicesCount;
        }
        public void setFailureDevicesCount(Integer failureDevicesCount)
        {
            this.failureDevicesCount = failureDevicesCount;
        }
    }
}

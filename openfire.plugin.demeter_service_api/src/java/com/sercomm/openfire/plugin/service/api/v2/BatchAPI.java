package com.sercomm.openfire.plugin.service.api.v2;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(BatchAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR, EndUserRole.OPERATOR})
public class BatchAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(BatchAPI.class);
    
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

    @POST
    @Path("batch")
    @Produces({MediaType.APPLICATION_JSON})
    public Response post(String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
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
    
    @PUT
    @Path("batch/{batchId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response put(
            @PathParam("batchId") String batchId,
            String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
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

    @GET
    @Path("batch/{batchId}/export")
    @Produces({MediaType.APPLICATION_JSON})
    public Response export(
            @PathParam("batchId") String batchId,
            String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
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

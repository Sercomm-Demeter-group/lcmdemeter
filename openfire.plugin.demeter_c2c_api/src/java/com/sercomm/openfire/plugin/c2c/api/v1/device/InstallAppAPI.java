package com.sercomm.openfire.plugin.c2c.api.v1.device;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.v1.InstallAppRequest;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.HttpServer;
import com.sercomm.openfire.plugin.c2c.exception.InternalErrorException;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.task.InstallAppTask;

@Path("umei/v1")
public class InstallAppAPI
{
    @POST
    @Path("device/{nodeName}/app")
    @Produces({MediaType.APPLICATION_JSON})
    public Response execute(
            @HeaderParam(HeaderField.HEADER_REQUEST_ID) String requestId,
            @HeaderParam(HeaderField.HEADER_ORIGINATOR_ID) String originatorId,
            @PathParam("nodeName") String nodeName,
            String requestPayload) 
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            if(false == NameRule.isDevice(nodeName))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID NODE NAME: " + nodeName;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(requestPayload))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "REQUEST PAYLOAD WAS BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            final String serial = NameRule.toDeviceSerial(nodeName);
            final String mac = NameRule.toDeviceMac(nodeName);

            String appId;
            String versionId;
            
            InstallAppRequest.RequestData requestData;
            try
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);
                
                requestData = bodyPayload.getDesire(
                    InstallAppRequest.RequestData.class);
                
                appId = requestData.getAppId();
                versionId = requestData.getVersionId();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            InstallMonitor monitor = new InstallMonitor();
            try
            {
                DeviceManager.getInstance().installApp(
                    serial, 
                    mac, 
                    appId, 
                    versionId, 
                    monitor);
            }
            catch(DemeterException e)
            {
                throw new UMEiException(e.getMessage(), Status.FORBIDDEN);
            }
            
            while(false == monitor.timeout &&
                  false == monitor.failed &&
                  false == monitor.completed)
            {
                Thread.sleep(100L);
            }
            
            if(true == monitor.timeout)
            {
                status = Response.Status.REQUEST_TIMEOUT;
                errorMessage = "DEVICE CANNOT INSTALL APP CAUSED BY INSTALLATION TIMEOUT";

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            if(true == monitor.failed)
            {
                status = Response.Status.NOT_ACCEPTABLE;
                errorMessage = "INSTALLATION ERROR: " + monitor.failedMessage;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(null)
                    .withData(null);

            // monitor.completed = true
            response = Response
                    .status(status)
                    .header(HeaderField.HEADER_REQUEST_ID, requestId)
                    .header(HeaderField.HEADER_ORIGINATOR_ID, originatorId)
                    .header(HeaderField.HEADER_RECEIVER_ID, HttpServer.SERVICE_NAME)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(bodyPayload.toString())
                    .build();            
        }
        catch(UMEiException e)
        {
            status = e.getErrorStatus();            
            errorMessage = e.getMessage();
            throw e.withIdentifyValues(
                requestId,
                originatorId,
                HttpServer.SERVICE_NAME);
        }
        catch(Throwable t)
        {
            status = Response.Status.INTERNAL_SERVER_ERROR;            
            errorMessage = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);
            throw new InternalErrorException(t.getMessage()).withIdentifyValues(
                requestId,
                HttpServer.SERVICE_NAME, 
                originatorId);
        }
        
        Log.write().info("({},{},{},{}); {}",
            requestId,
            originatorId,
            nodeName,
            requestPayload,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());
        
        return response;
    }
    
    private static class InstallMonitor implements InstallAppTask.Listener
    {
        public boolean completed = false;
        public boolean timeout = false;

        public boolean failed = false;
        public String failedMessage = null;
        
        @Override
        public void onDelivered(String serial, String mac, String appId, String versionId, long triggerTime)
        {
        }

        @Override
        public void onInstalling(String serial, String mac, String appId, String versionId, long triggerTime)
        {
        }

        @Override
        public void onCompleted(String serial, String mac, String appId, String versionId, long triggerTime)
        {
            this.completed = true;
        }

        @Override
        public void onTimeout(String serial, String mac, String appId, String versionId, long triggerTime)
        {
            this.timeout = true;
        }

        @Override
        public void onFail(String serial, String mac, String appId, String versionId, String errorMessage, long triggerTime)
        {
            this.failed = true;
            this.failedMessage = errorMessage;
        }
    }
}
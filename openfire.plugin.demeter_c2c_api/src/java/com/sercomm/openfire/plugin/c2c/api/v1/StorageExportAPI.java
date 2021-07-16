package com.sercomm.openfire.plugin.c2c.api.v1;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.HttpServer;
import com.sercomm.openfire.plugin.c2c.exception.InternalErrorException;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;

@Path("umei/v1")
public class StorageExportAPI
{
    private static final Logger log = LoggerFactory.getLogger(StorageExportAPI.class);

    @POST
    @Path("export")
    @Produces({MediaType.APPLICATION_JSON})
    public Response execute(
            @HeaderParam(HeaderField.HEADER_REQUEST_ID) String requestId,
            @HeaderParam(HeaderField.HEADER_ORIGINATOR_ID) String originatorId,
            String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            response = Response
                    .status(status)
                    .header(HeaderField.HEADER_REQUEST_ID, requestId)
                    .header(HeaderField.HEADER_ORIGINATOR_ID, originatorId)
                    .header(HeaderField.HEADER_RECEIVER_ID, HttpServer.SERVICE_NAME)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("Ok")
                    .build();
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

        log.info("({},{},{}); {}",
            requestId,
            originatorId,
            requestPayload,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        Runnable task = () -> {
            try{
                AppManager.exportAppsToCloud();
            }
            catch(Throwable ignored){}
        };

        Thread thread = new Thread(task);
        thread.start();

        return response;
    }
}

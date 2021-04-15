package com.sercomm.openfire.plugin.c2c.api.v1.app;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.v1.GetInstallableAppResult;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.HttpServer;
import com.sercomm.openfire.plugin.c2c.exception.InternalErrorException;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;

@Path("umei/v1")
public class GetAppAPI
{
    private static final Logger log = LoggerFactory.getLogger(GetAppAPI.class);

    @GET
    @Path("app/{appId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response execute(
            @HeaderParam(HeaderField.HEADER_REQUEST_ID) String requestId,
            @HeaderParam(HeaderField.HEADER_ORIGINATOR_ID) String originatorId,
            @PathParam("appId") String appId) 
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            if(true == XStringUtil.isBlank(appId))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "APP ID CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            App app = AppManager.getInstance().getApp(appId);
            if(null == app)
            {
                status = Response.Status.NOT_FOUND;
                errorMessage = "APP CANNOT BE FOUND";

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            GetInstallableAppResult.ResultData data = new GetInstallableAppResult.ResultData();
            data.setAppName(app.getName());
            data.setPublisher(app.getPublisher());
            data.setAppId(app.getId());
            data.setCreationTime(DateTime.from(app.getCreationTime()).toString(DateTime.FORMAT_ISO));

            List<GetInstallableAppResult.ResultData.Version> versions = new ArrayList<>();
            
            List<AppVersion> appVersions = AppManager.getInstance().getAppVersions(app.getId());
            for(AppVersion appVersion : appVersions)
            {
                GetInstallableAppResult.ResultData.Version version =
                        new GetInstallableAppResult.ResultData.Version();

                version.setVersionName(appVersion.getVersion());
                version.setVersionId(appVersion.getId());
                version.setCreationTime(DateTime.from(appVersion.getCreationTime()).toString(DateTime.FORMAT_ISO));

                versions.add(version);
            }
            data.setVersions(versions);

            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(null)
                    .withData(data);
            
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
        
        log.info("({},{},{}); {}",
            requestId,
            originatorId,
            appId,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }
}

package com.sercomm.openfire.plugin.c2c.api.v1.device;

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

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.v1.GetInstalledAppsResult;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.HttpServer;
import com.sercomm.openfire.plugin.c2c.exception.InternalErrorException;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppInstallation;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;
import com.sercomm.openfire.plugin.exception.DemeterException;

@Path("umei/v1")
public class GetInstalledAppsAPI
{
    @GET
    @Path("device/{nodeName}/apps")
    @Produces({MediaType.APPLICATION_JSON})
    public Response execute(
            @HeaderParam(HeaderField.HEADER_REQUEST_ID) String requestId,
            @HeaderParam(HeaderField.HEADER_ORIGINATOR_ID) String originatorId,
            @PathParam("nodeName") String nodeName) 
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

            final String serial = NameRule.toDeviceSerial(nodeName);
            final String mac = NameRule.toDeviceMac(nodeName);
            
            List<GetInstalledAppsResult.ResultData> data = new ArrayList<>();
            try
            {
                List<String> versionIds = DeviceManager.getInstance().getInstalledVersionIds(serial, mac);
                for(String versionId : versionIds)
                {
                    AppVersion appVersion = AppManager.getInstance().getAppVersion(versionId);
                    
                    String appId = appVersion.getAppId();
                    App app = AppManager.getInstance().getApp(appId);
                    
                    AppInstallation appInstallation = DeviceManager.getInstance().getInstalledApp(serial, mac, app.getName());

                    GetInstalledAppsResult.ResultData.Version version = new GetInstalledAppsResult.ResultData.Version();
                    version.setVersionName(appInstallation.getVersion());
                    version.setVersionId(versionId);
                    
                    GetInstalledAppsResult.ResultData row = new GetInstalledAppsResult.ResultData();
                    row.setAppName(app.getName());
                    row.setPublisher(app.getPublisher());
                    row.setAppId(app.getId());
                    row.setStatus(appInstallation.getStatus());
                    row.setVersion(version);
                    
                    data.add(row);
                }
            }
            catch(DemeterException e)
            {
                throw new UMEiException(e.getMessage(), Status.FORBIDDEN);
            }
                        
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

        Log.write().info("({},{},{}); {}",
            requestId,
            originatorId,
            nodeName,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());
        
        return response;
    }
}

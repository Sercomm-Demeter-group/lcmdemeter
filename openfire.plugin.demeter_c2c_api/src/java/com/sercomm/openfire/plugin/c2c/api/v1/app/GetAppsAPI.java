package com.sercomm.openfire.plugin.c2c.api.v1.app;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.umei.Meta;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.v1.GetInstallableAppsResult;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.HttpServer;
import com.sercomm.openfire.plugin.c2c.exception.InternalErrorException;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;

@Path("umei/v1")
public class GetAppsAPI
{
    private static final Logger log = LoggerFactory.getLogger(GetAppsAPI.class);

    @GET
    @Path("apps")
    @Produces({MediaType.APPLICATION_JSON})
    public Response execute(
            @HeaderParam(HeaderField.HEADER_REQUEST_ID) String requestId,
            @HeaderParam(HeaderField.HEADER_ORIGINATOR_ID) String originatorId,
            @QueryParam("model") String model,
            @QueryParam("from") Integer from,
            @QueryParam("size") Integer size,
            @QueryParam("sort") String sort) 
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            if(true == XStringUtil.isBlank(model))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MODEL NAME CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            int fromValue = 0;
            int sizeValue = 100;

            if(null != from)
            {
                fromValue = from;
            }
            
            if(null != size)
            {
                if(size > 500)
                {
                    status = Response.Status.FORBIDDEN;
                    errorMessage = "'size' VALUE CANNOT BE LARGER THAN 500";

                    throw new UMEiException(
                        errorMessage,
                        status);
                }
                
                sizeValue = size;
            }

            List<String> modelNames = DeviceManager.getInstance().getAvailableModelNames();
            if(false == modelNames.contains(model))
            {
                status = Response.Status.NOT_FOUND;
                errorMessage = "MODEL NAME CANNOT BE FOUND";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(sort))
            {
                sort = "name:asc";
            }
            
            String order;
            String[] sortTokens = sort.split(":");
            if(2 == sortTokens.length)
            {
                order = sortTokens[1];
            }
            else
            {
                order = "asc";
            }
            
            String sortByColumn = sortTokens[0];

            List<App> apps = new ArrayList<>();
            int totalCount = AppManager.getInstance().getAppsByModel(
                model, fromValue, sizeValue, sortByColumn, order, apps);

            List<GetInstallableAppsResult.ResultData> data = new ArrayList<>();
            for(App app : apps)
            {
                GetInstallableAppsResult.ResultData row = new GetInstallableAppsResult.ResultData();
                row.setAppName(app.getName());
                row.setPublisher(app.getPublisher());
                row.setAppId(app.getId());
                row.setCreationTime(DateTime.from(app.getCreationTime()).toString(DateTime.FORMAT_ISO));
                
                List<GetInstallableAppsResult.ResultData.Version> versions = new ArrayList<>();
                
                List<AppVersion> appVersions = AppManager.getInstance().getAppVersions(app.getId());
                for(AppVersion appVersion : appVersions)
                {
                    GetInstallableAppsResult.ResultData.Version version =
                            new GetInstallableAppsResult.ResultData.Version();

                    version.setVersionName(appVersion.getVersion());
                    version.setVersionId(appVersion.getId());
                    version.setCreationTime(DateTime.from(appVersion.getCreationTime()).toString(DateTime.FORMAT_ISO));

                    versions.add(version);
                }
                row.setVersions(versions);
                
                data.add(row);
            }
            
            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(new Meta().withFrom(fromValue).withSize(data.size()).withTotal(totalCount))
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
        
        log.info("({},{},{},{},{},{}); {}",
            requestId,
            originatorId,
            model,
            from,
            size,
            sort,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }
}

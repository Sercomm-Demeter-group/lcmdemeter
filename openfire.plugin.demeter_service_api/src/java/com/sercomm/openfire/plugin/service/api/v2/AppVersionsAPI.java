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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.Meta;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(AppVersionsAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR})
public class AppVersionsAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(AppVersionsAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;

    @GET
    @Path("application/{applicationId}/versions")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get(
            @PathParam("applicationId") String applicationId,
            @QueryParam("from") Integer from,
            @QueryParam("size") Integer size,
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
            // check if the application ID is valid
            try
            {
                AppManager.getInstance().getApp(applicationId);
            }
            catch(DemeterException e)
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = e.getMessage();

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            List<Object> arguments = new ArrayList<>();
            
            int totalCount = 0;
            // query total rows count
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT COUNT(*) AS `count` FROM `sAppVersion` WHERE `appId` = ? ");
            arguments.add(applicationId);

            if(!filters.isEmpty())
            {
                builder.append("AND ");
            }

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
                    case "name":
                        builder.append("`version` LIKE ? ");
                        arguments.add(tokens[1] + "%");
                        break;
                    case "status":
                        builder.append("`status` = ? ");
                        switch(tokens[1])
                        {
                            case "enable":
                                arguments.add(1);
                                break;
                            case "disable":
                                arguments.add(0);
                                break;
                            default:
                                throw new UMEiException(
                                    "INVALID FILTER ATTRIBUTE VALUE: " + tokens[1],
                                    Response.Status.BAD_REQUEST);
                        }
                        break;
                    default:
                        throw new UMEiException(
                            "INVALID FILTER ATTRIBUTE: " + tokens[0],
                            Response.Status.BAD_REQUEST);
                }

                if(iterator.hasNext())
                {
                    builder.append("AND ");
                }
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(builder.toString());

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
            
            // query rows
            builder = new StringBuilder();
            arguments.clear();

            builder.append("SELECT * FROM `sAppVersion` WHERE `appId` = ? ");
            arguments.add(applicationId);

            if(!filters.isEmpty())
            {
                builder.append("AND ");
            }

            iterator = filters.iterator();
            while(iterator.hasNext())
            {
                final String filter = iterator.next();

                String[] tokens = filter.split(":", 2);
                switch(tokens[0])
                {
                    case "name":
                        builder.append("`version` LIKE ? ");
                        arguments.add(tokens[1] + "%");
                        break;
                    case "status":
                        builder.append("`status` = ? ");
                        switch(tokens[1])
                        {
                            case "enable":
                                arguments.add(1);
                                break;
                            case "disable":
                                arguments.add(0);
                                break;
                        }
                        break;
                }

                if(iterator.hasNext())
                {
                    builder.append("AND ");
                }
            }

            builder.append("ORDER BY `creationTime` DESC ");

            if(null != from && null != size)
            {
                builder.append("LIMIT ")
                .append(Integer.toString(from))
                .append(",")
                .append(Integer.toString(size));
            }
            
            List<GetAppVersionResult> result = null;
            try
            {
                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(builder.toString());
                for(int idx = 0; idx < arguments.size(); idx++)
                {
                    stmt.setObject(idx + 1, arguments.get(idx));
                }

                rs = stmt.executeQuery();
                
                result = new ArrayList<>();
                
                while(rs.next())
                {
                    GetAppVersionResult object = new GetAppVersionResult();
                    object.setVersionId(rs.getString("id"));
                    object.setName(rs.getString("version"));
                    object.setStatus(rs.getInt("status") == 1 ? "enable" : "disable");
                    object.setCreationTime(DateTime.from(rs.getLong("creationTime")).toString(DateTime.FORMAT_ISO_MS));

                    result.add(object);
                }
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
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

        log.info("({},{},{},{},{},{}); {}",
            userId,
            sessionId,
            applicationId,
            from,
            size,
            filters,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }
    
    public static class GetAppVersionResult
    {
        private String versionId;
        private String name;
        private String status;
        private String creationTime;

        public String getVersionId()
        {
            return versionId;
        }
        public void setVersionId(String versionId)
        {
            this.versionId = versionId;
        }
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
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
    }
}

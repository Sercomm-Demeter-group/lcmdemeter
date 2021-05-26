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

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.Meta;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(DeviceModelsAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR})
public class DeviceModelsAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(DeviceModelsAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;
    
    @GET
    @Path("models")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get(
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
            int totalCount = 0;

            // query total rows count
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT COUNT(*) AS `count` FROM `sDeviceModel` ");
            
            if(!filters.isEmpty())
            {
                builder.append("WHERE ");
            }

            Iterator<String> iterator = filters.iterator();
            while(iterator.hasNext())
            {
                final String filter = iterator.next();

                String[] tokens = filter.split(":", 2);
                if(2 != tokens.length)
                {
                    continue;
                }

                if(0 != tokens[0].compareTo("name"))
                {
                    status = Response.Status.BAD_REQUEST;
                    errorMessage = "INVALID FILTER: " + tokens[0];

                    throw new UMEiException(
                        errorMessage,
                        status);
                }

                // locate to the target column
                if(0 == tokens[0].compareTo("name"))
                {
                    tokens[0] = "modelName";
                }

                builder.append("`").append(tokens[0]).append("` LIKE ")
                    .append("'").append(tokens[1]).append("%' ");

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
            builder.append("SELECT * FROM `sDeviceModel` ");
            
            if(!filters.isEmpty())
            {
                builder.append("WHERE ");
            }
            
            iterator = filters.iterator();
            while(iterator.hasNext())
            {
                final String filter = iterator.next();

                String[] tokens = filter.split(":", 2);
                if(2 != tokens.length)
                {
                    continue;
                }

                // since filters have been verified when querying rows count
                // it is not necessary to verify again here

                // locate to the target column
                if(0 == tokens[0].compareTo("name"))
                {
                    tokens[0] = "modelName";
                }

                builder.append("`").append(tokens[0]).append("` LIKE ")
                    .append("'").append(tokens[1]).append("%' ");

                if(iterator.hasNext())
                {
                    builder.append("AND ");
                }
            }
            
            List<GetDeviceModelResult> result = null;
            try
            {
                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(builder.toString());
                rs = stmt.executeQuery();
                
                result = new ArrayList<>();
                
                while(rs.next())
                {
                    GetDeviceModelResult object = new GetDeviceModelResult();
                    object.setModelId(rs.getString("uuid"));
                    object.setName(rs.getString("modelName"));
                    object.setStatus(1 == rs.getInt("status") ? "enable" : "disable");
                    object.setCreationTime(DateTime.from(rs.getLong("creationTime")).toString(DateTime.FORMAT_ISO_MS));
                    object.setUpdatedTime(DateTime.from(rs.getLong("updatedTime")).toString(DateTime.FORMAT_ISO_MS));

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

        log.info("({},{},{},{},{}); {}",
            userId,
            sessionId,
            from,
            size,
            filters,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }
    
    public static class GetDeviceModelResult
    {
        private String modelId;
        private String name;
        private String status;
        private String creationTime;
        private String updatedTime;

        public String getModelId()
        {
            return modelId;
        }
        public void setModelId(String modelId)
        {
            this.modelId = modelId;
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
        public String getUpdatedTime()
        {
            return updatedTime;
        }
        public void setUpdatedTime(String updatedTime)
        {
            this.updatedTime = updatedTime;
        }
        
    }
}

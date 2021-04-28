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

@Path(AppsAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR})
public class AppsAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(AppsAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;

    @GET
    @Path("applications")
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
            List<Object> arguments = new ArrayList<>();

            int totalCount = 0;
            // query total rows count
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT COUNT(*) AS `count` FROM `sApp` ");
            
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
                    throw new UMEiException(
                        "INVALID FILTER: " + filter,
                        Response.Status.BAD_REQUEST);
                }

                switch(tokens[0])
                {
                    case "name":
                        builder.append("`name` LIKE ? ");
                        arguments.add(tokens[1] + "%");
                        break;
                    case "publisher":
                        builder.append("`publisher` LIKE ? ");
                        arguments.add(tokens[1] + "%");
                        break;
                    case "model":
                        builder.append("`model` LIKE ? ");
                        arguments.add(tokens[1] + "%");
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

            builder.append("SELECT * FROM `sApp` ");

            if(!filters.isEmpty())
            {
                builder.append("WHERE ");
            }

            iterator = filters.iterator();
            while(iterator.hasNext())
            {
                final String filter = iterator.next();

                String[] tokens = filter.split(":", 2);
                switch(tokens[0])
                {
                    case "name":
                        builder.append("`name` LIKE ? ");
                        arguments.add(tokens[1] + "%");
                        break;
                    case "publisher":
                        builder.append("`publisher` LIKE ? ");
                        arguments.add(tokens[1] + "%");
                        break;
                    case "model":
                        builder.append("`model` LIKE ? ");
                        arguments.add(tokens[1] + "%");
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
            
            if(null != from && null != size)
            {
                builder.append("LIMIT ")
                .append(Integer.toString(from))
                .append(",")
                .append(Integer.toString(size));
            }

            List<GetApplicationResult> result = null;
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
                    GetApplicationResult object = new GetApplicationResult();
                    object.setApplicationId(rs.getString("id"));
                    object.setName(rs.getString("name"));
                    object.setPublisher(rs.getString("publisher"));
                    object.setModel(rs.getString("model"));
                    object.setDescription(rs.getString("description"));
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
                        .withFrom(from)
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
    
    public static class GetApplicationResult
    {
        private String applicationId;
        private String name;
        private String publisher;
        private String model;
        private String description;
        private String creationTime;

        public String getApplicationId()
        {
            return applicationId;
        }
        public void setApplicationId(String applicationId)
        {
            this.applicationId = applicationId;
        }
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getPublisher()
        {
            return publisher;
        }
        public void setPublisher(String publisher)
        {
            this.publisher = publisher;
        }
        public String getModel()
        {
            return model;
        }
        public void setModel(String model)
        {
            this.model = model;
        }
        public String getDescription()
        {
            return description;
        }
        public void setDescription(String description)
        {
            this.description = description;
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

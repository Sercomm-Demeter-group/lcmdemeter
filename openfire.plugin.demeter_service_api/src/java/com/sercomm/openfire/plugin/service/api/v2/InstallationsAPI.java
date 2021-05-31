package com.sercomm.openfire.plugin.service.api.v2;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(InstallationsAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR, EndUserRole.OPERATOR})
public class InstallationsAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(InstallationsAPI.class);
    
    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;
}

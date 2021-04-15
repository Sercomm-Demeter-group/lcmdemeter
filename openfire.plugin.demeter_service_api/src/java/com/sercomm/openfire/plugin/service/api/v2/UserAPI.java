package com.sercomm.openfire.plugin.service.api.v2;

import javax.ws.rs.Path;

import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(UserAPI.URI_PATH)
public class UserAPI extends ServiceAPIBase
{
    protected final static String URI_PATH = ServiceAPIBase.URI_PATH + "v2/"; 
}

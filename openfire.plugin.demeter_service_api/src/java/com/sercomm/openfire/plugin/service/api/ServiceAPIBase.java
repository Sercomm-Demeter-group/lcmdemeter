package com.sercomm.openfire.plugin.service.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;

public class ServiceAPIBase
{
    protected final static String URI_PATH = "api/";
        
    protected final static class ErrorResponseModel
    {
        public int code;
        public String title = XStringUtil.BLANK;
        public String message = XStringUtil.BLANK;
    }
        
    protected Response createError(
            Response.Status status,
            String title,
            String errorMessage)
    {
        ResponseBuilder responseBuilder = Response.status(status);
        ErrorResponseModel errorResponseModel = new ErrorResponseModel();
        errorResponseModel.code = status.getStatusCode();
        errorResponseModel.title = title;
        errorResponseModel.message = errorMessage;
        
        responseBuilder.entity(Json.build(errorResponseModel));
        return responseBuilder.build();
    }
}

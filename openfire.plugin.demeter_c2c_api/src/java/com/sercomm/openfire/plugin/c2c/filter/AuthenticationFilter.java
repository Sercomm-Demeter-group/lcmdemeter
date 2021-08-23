package com.sercomm.openfire.plugin.c2c.filter;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.Priorities;

import com.sercomm.openfire.plugin.c2c.exception.UMEiException;

import com.sercomm.openfire.plugin.SystemProperties;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String apiKey = SystemProperties.getInstance().getApiKey();
        if(apiKey.length() == 0) {
            return;
        }

        // Get the Authorization header from the request
        String authorizationHeader =
                requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Validate the Authorization header
        if (!isTokenBasedAuthentication(authorizationHeader)) {
            throw new UMEiException(
                "Unathorized: Authorization Header 'Bearer' is Missing",
                Status.UNAUTHORIZED);
        }

        // Extract the token from the Authorization header
        String token = authorizationHeader
                            .substring(AUTHENTICATION_SCHEME.length()).trim();

        // Validate the token
        if(!apiKey.equals(token)) {
            throw new UMEiException(
                "Unauthorized: Bad API Token",
                Status.UNAUTHORIZED);
        }
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {

        // Check if the Authorization header is valid
        // It must not be null and must be prefixed with "Bearer" plus a whitespace
        // The authentication scheme comparison must be case-insensitive
        return authorizationHeader != null && authorizationHeader.toLowerCase()
                    .startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }
}

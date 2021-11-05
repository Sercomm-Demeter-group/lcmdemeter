package com.sercomm.demeter.microservices.client.v1;

import java.nio.charset.StandardCharsets;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.HttpUtil;
import com.sercomm.commons.util.XStringUtil;

public class RESTfulClient
{
    private String endpoint;
    private String bearerToken;
    private boolean enableSSL = false;

    private RESTfulClient(String endpoint, String bearerToken, boolean enableSSL)
    {
        this.endpoint = endpoint;
        this.enableSSL = enableSSL;
        this.bearerToken = bearerToken;
    }
    
    public static class Builder
    {
        private String endpoint;
        private String bearerToken;
        private boolean enableSSL = false;

        public Builder endpoint(String endpoint)
        {
            this.endpoint = endpoint;
            return this;
        }

        public Builder bearerToken(String bearerToken)
        {
            this.bearerToken = bearerToken;
            return this;
        }

        public Builder enableSSL(boolean enableSSL)
        {
            this.enableSSL = enableSSL;
            return this;
        }
        
        public RESTfulClient build()
        {
            return new RESTfulClient(this.endpoint, this.bearerToken, this.enableSSL);
        }
    }
    
    public PostEchoResult postEcho(
            PostEchoRequest postEchoRequest)
    {
        PostEchoResult postEchoResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/echo");
            
            HttpPost httpPost = new HttpPost(builder.build());
                        
            httpPost.addHeader(HeaderField.HEADER_REQUEST_ID, postEchoRequest.getRequestId());
            httpPost.addHeader(HeaderField.HEADER_ORIGINATOR_ID, postEchoRequest.getOriginatorId());
            httpPost.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            StringEntity requestEntity = new StringEntity(postEchoRequest.getRawText());
            requestEntity.setContentType("application/json");
            httpPost.setEntity(requestEntity);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpPost))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                postEchoResult = new PostEchoResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return postEchoResult;
    }

    public PostUserResult postUser(
            PostUserRequest postUserRequest)
    {
        PostUserResult postUserResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/user");
            
            HttpPost httpPost = new HttpPost(builder.build());
                        
            httpPost.addHeader(HeaderField.HEADER_REQUEST_ID, postUserRequest.getRequestId());
            httpPost.addHeader(HeaderField.HEADER_ORIGINATOR_ID, postUserRequest.getOriginatorId());
            httpPost.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);
            
            StringEntity requestEntity = new StringEntity(postUserRequest.getRawText());
            requestEntity.setContentType("application/json");
            httpPost.setEntity(requestEntity);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpPost))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                postUserResult = new PostUserResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return postUserResult;
    }

    public PostUbusCommandResult postUbusCommand(
            PostUbusCommandRequest postUbusCommandRequest)
    {
        PostUbusCommandResult postUbusCommandResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + postUbusCommandRequest.getNodeName() + "/ubus");
            
            HttpPost httpPost = new HttpPost(builder.build());
                        
            httpPost.addHeader(HeaderField.HEADER_REQUEST_ID, postUbusCommandRequest.getRequestId());
            httpPost.addHeader(HeaderField.HEADER_ORIGINATOR_ID, postUbusCommandRequest.getOriginatorId());
            httpPost.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);
            
            StringEntity requestEntity = new StringEntity(postUbusCommandRequest.getRawText());
            requestEntity.setContentType("application/json");
            httpPost.setEntity(requestEntity);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpPost))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                postUbusCommandResult = new PostUbusCommandResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return postUbusCommandResult;
    }

    public GetDevicesResult getDevices(
            GetDevicesRequest getDevicesRequest)
    {
        GetDevicesResult getDevicesResult = null;
        
        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/devices");
            
            if(XStringUtil.isNotBlank(getDevicesRequest.getModels()))
            {
                builder.addParameter("models", getDevicesRequest.getModels());
            }
            
            if(XStringUtil.isNotBlank(getDevicesRequest.getStates()))
            {
                builder.addParameter("states", getDevicesRequest.getStates());
            }
            
            if(null != getDevicesRequest.getFrom())
            {
                builder.addParameter("from", getDevicesRequest.getFrom().toString());
            }
            
            if(null != getDevicesRequest.getSize())
            {
                builder.addParameter("size", getDevicesRequest.getSize().toString());
            }
            
            if(XStringUtil.isNotBlank(getDevicesRequest.getSort()))
            {
                builder.addParameter("sort", getDevicesRequest.getSort());
            }
            
            HttpGet httpGet = new HttpGet(builder.build());
                        
            httpGet.addHeader(HeaderField.HEADER_REQUEST_ID, getDevicesRequest.getRequestId());
            httpGet.addHeader(HeaderField.HEADER_ORIGINATOR_ID, getDevicesRequest.getOriginatorId());
            httpGet.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpGet))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                getDevicesResult = new GetDevicesResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }

        return getDevicesResult;
    }

    public GetDeviceResult getDevice(
            GetDeviceRequest getDeviceRequest)
    {
        GetDeviceResult getDeviceResult = null;
        
        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + getDeviceRequest.getNodeName());
                        
            HttpGet httpGet = new HttpGet(builder.build());
                        
            httpGet.addHeader(HeaderField.HEADER_REQUEST_ID, getDeviceRequest.getRequestId());
            httpGet.addHeader(HeaderField.HEADER_ORIGINATOR_ID, getDeviceRequest.getOriginatorId());
            httpGet.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpGet))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                getDeviceResult = new GetDeviceResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }

        return getDeviceResult;
    }

    public GetInstallableAppsResult getInstallableApps(
            GetInstallableAppsRequest getInstallableAppsRequest)
    {
        GetInstallableAppsResult getInstallableAppsResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/apps");

            builder.addParameter("model", getInstallableAppsRequest.getModel());
            
            if(null != getInstallableAppsRequest.getFrom())
            {
                builder.addParameter("from", getInstallableAppsRequest.getFrom().toString());
            }
            
            if(null != getInstallableAppsRequest.getSize())
            {
                builder.addParameter("size", getInstallableAppsRequest.getSize().toString());
            }
            
            if(XStringUtil.isNotBlank(getInstallableAppsRequest.getSort()))
            {
                builder.addParameter("sort", getInstallableAppsRequest.getSort());
            }

            HttpGet httpGet = new HttpGet(builder.build());
                        
            httpGet.addHeader(HeaderField.HEADER_REQUEST_ID, getInstallableAppsRequest.getRequestId());
            httpGet.addHeader(HeaderField.HEADER_ORIGINATOR_ID, getInstallableAppsRequest.getOriginatorId());
            httpGet.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpGet))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                getInstallableAppsResult = new GetInstallableAppsResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return getInstallableAppsResult;
    }

    public GetInstallableAppResult getInstallableApp(
            GetInstallableAppRequest getInstallableAppRequest)
    {
        GetInstallableAppResult getInstallableAppResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/app/" + getInstallableAppRequest.getAppId());

            HttpGet httpGet = new HttpGet(builder.build());
                        
            httpGet.addHeader(HeaderField.HEADER_REQUEST_ID, getInstallableAppRequest.getRequestId());
            httpGet.addHeader(HeaderField.HEADER_ORIGINATOR_ID, getInstallableAppRequest.getOriginatorId());
            httpGet.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpGet))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                getInstallableAppResult = new GetInstallableAppResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return getInstallableAppResult;
    }

    public GetInstallableAppByNameResult getInstallableAppByName(
            GetInstallableAppByNameRequest getInstallableAppByNameRequest)
    {
        GetInstallableAppByNameResult getInstallableAppByNameResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/app");

            builder.addParameter("model", getInstallableAppByNameRequest.getModel());
            builder.addParameter("name", getInstallableAppByNameRequest.getAppName());
            builder.addParameter("publisher", getInstallableAppByNameRequest.getAppPublisher());

            HttpGet httpGet = new HttpGet(builder.build());
                        
            httpGet.addHeader(HeaderField.HEADER_REQUEST_ID, getInstallableAppByNameRequest.getRequestId());
            httpGet.addHeader(HeaderField.HEADER_ORIGINATOR_ID, getInstallableAppByNameRequest.getOriginatorId());
            httpGet.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpGet))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                getInstallableAppByNameResult = new GetInstallableAppByNameResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return getInstallableAppByNameResult;
    }

    public InstallAppResult installApp(
            InstallAppRequest installAppRequest)
    {
        InstallAppResult installAppResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + installAppRequest.getNodeName() + "/app");
            
            HttpPost httpPost = new HttpPost(builder.build());
                        
            httpPost.addHeader(HeaderField.HEADER_REQUEST_ID, installAppRequest.getRequestId());
            httpPost.addHeader(HeaderField.HEADER_ORIGINATOR_ID, installAppRequest.getOriginatorId());
            httpPost.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);
            
            StringEntity requestEntity = new StringEntity(installAppRequest.getRawText());
            requestEntity.setContentType("application/json");
            httpPost.setEntity(requestEntity);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpPost))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                installAppResult = new InstallAppResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return installAppResult;
    }

    public UninstallAppResult uninstallApp(
            UninstallAppRequest uninstallAppRequest)
    {
        UninstallAppResult uninstallAppResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + uninstallAppRequest.getNodeName() + "/app/" + uninstallAppRequest.getAppId());
            
            HttpDelete httpDelete = new HttpDelete(builder.build());
                        
            httpDelete.addHeader(HeaderField.HEADER_REQUEST_ID, uninstallAppRequest.getRequestId());
            httpDelete.addHeader(HeaderField.HEADER_ORIGINATOR_ID, uninstallAppRequest.getOriginatorId());
            httpDelete.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);
            
            try(CloseableHttpResponse httpResponse = httpClient.execute(httpDelete))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                uninstallAppResult = new UninstallAppResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return uninstallAppResult;
    }

    public UpdateAppResult updateApp(
            UpdateAppRequest updateAppRequest)
    {
        UpdateAppResult updateAppResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + updateAppRequest.getNodeName() + "/app");
            
            HttpPut httpPut = new HttpPut(builder.build());
                        
            httpPut.addHeader(HeaderField.HEADER_REQUEST_ID, updateAppRequest.getRequestId());
            httpPut.addHeader(HeaderField.HEADER_ORIGINATOR_ID, updateAppRequest.getOriginatorId());
            httpPut.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);
            
            StringEntity requestEntity = new StringEntity(updateAppRequest.getRawText());
            requestEntity.setContentType("application/json");
            httpPut.setEntity(requestEntity);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpPut))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                updateAppResult = new UpdateAppResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return updateAppResult;
    }

    public GetInstalledAppResult getInstalledApp(
            GetInstalledAppRequest getInstalledAppRequest)
    {
        GetInstalledAppResult getInstalledAppResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + getInstalledAppRequest.getNodeName() + "/app/" + getInstalledAppRequest.getAppId());
            
            HttpGet httpGet = new HttpGet(builder.build());
                        
            httpGet.addHeader(HeaderField.HEADER_REQUEST_ID, getInstalledAppRequest.getRequestId());
            httpGet.addHeader(HeaderField.HEADER_ORIGINATOR_ID, getInstalledAppRequest.getOriginatorId());
            httpGet.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);
            
            try(CloseableHttpResponse httpResponse = httpClient.execute(httpGet))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                getInstalledAppResult = new GetInstalledAppResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return getInstalledAppResult;
    }

    public GetInstalledAppsResult getInstalledApps(
            GetInstalledAppsRequest getInstalledAppsRequest)
    {
        GetInstalledAppsResult getInstalledAppsResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + getInstalledAppsRequest.getNodeName() + "/app");
            
            HttpGet httpGet = new HttpGet(builder.build());
                        
            httpGet.addHeader(HeaderField.HEADER_REQUEST_ID, getInstalledAppsRequest.getRequestId());
            httpGet.addHeader(HeaderField.HEADER_ORIGINATOR_ID, getInstalledAppsRequest.getOriginatorId());
            httpGet.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpGet))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                getInstalledAppsResult = new GetInstalledAppsResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return getInstalledAppsResult;
    }

    public StartAppResult startApp(
            StartAppRequest startAppRequest)
    {
        StartAppResult startAppResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + startAppRequest.getNodeName() + "/app/" + startAppRequest.getAppId() + "/start");
            
            HttpPut httpPut = new HttpPut(builder.build());
                        
            httpPut.addHeader(HeaderField.HEADER_REQUEST_ID, startAppRequest.getRequestId());
            httpPut.addHeader(HeaderField.HEADER_ORIGINATOR_ID, startAppRequest.getOriginatorId());
            httpPut.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpPut))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                startAppResult = new StartAppResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return startAppResult;
    }

    public StopAppResult stopApp(
            StopAppRequest stopAppRequest)
    {
        StopAppResult stopAppResult = null;

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            URIBuilder builder = new URIBuilder()
                    .setScheme(this.enableSSL ? "https" : "http")
                    .setHost(this.endpoint)
                    .setPath("/umei/v1/device/" + stopAppRequest.getNodeName() + "/app/" + stopAppRequest.getAppId() + "/stop");
            
            HttpPut httpPut = new HttpPut(builder.build());
                        
            httpPut.addHeader(HeaderField.HEADER_REQUEST_ID, stopAppRequest.getRequestId());
            httpPut.addHeader(HeaderField.HEADER_ORIGINATOR_ID, stopAppRequest.getOriginatorId());
            httpPut.addHeader(HttpUtil.HEADER_AUTHORIZATION, "Bearer " + this.bearerToken);

            try(CloseableHttpResponse httpResponse = httpClient.execute(httpPut))
            {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                BodyPayload resultPayload = null;
                if(HttpStatus.SC_OK == statusCode)
                {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    resultPayload = BodyPayload.from(resultString);
                }

                Header requestId = httpResponse.getFirstHeader(HeaderField.HEADER_REQUEST_ID);
                Header originatorId = httpResponse.getFirstHeader(HeaderField.HEADER_ORIGINATOR_ID);
                Header receiverId = httpResponse.getFirstHeader(HeaderField.HEADER_RECEIVER_ID);

                stopAppResult = new StopAppResult()
                        .withRequestId(null != requestId ? requestId.getValue() : XStringUtil.BLANK)
                        .withOriginatorId(null != originatorId ? originatorId.getValue() : XStringUtil.BLANK)
                        .withReceiverId(null != receiverId ? receiverId.getValue() : XStringUtil.BLANK)
                        .withStatusCode(statusCode)
                        .withBodyPayload(resultPayload);                
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return stopAppResult;
    }
}

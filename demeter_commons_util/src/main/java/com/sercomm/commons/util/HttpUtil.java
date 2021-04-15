package com.sercomm.commons.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil
{
    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    ///////////////////////////////////////////////////////////////////////////////////////
    public final static String SESSION_ID                   = "SERCOMMID";
    ///////////////////////////////////////////////////////////////////////////////////////
    public final static String METHOD_GET                   = "GET";
    public final static String METHOD_POST                  = "POST";
    public final static String METHOD_HEAD                  = "HEAD";
    public final static String METHOD_PUT                   = "PUT";
    public final static String METHOD_DELETE                = "DELETE";
    ///////////////////////////////////////////////////////////////////////////////////////
    public final static String HEADER_EOL                   = "\r\n";
    public final static String HEADER_EOH                   = "\r\n\r\n";
    public final static String HEADER_USER_AGENT            = "User-Agent";
    public final static String HEADER_ACCEPT                = "Accept";
    public final static String HEADER_ACCEPT_CHARSET        = "Accept-Charset";
    public final static String HEADER_CONTENT_TYPE          = "Content-Type";
    public final static String HEADER_CONTENT_LENGTH        = "Content-Length";
    public final static String HEADER_CONTEXT_DISPOS        = "Content-Disposition";
    public final static String HEADER_SET_COOKIE            = "Set-Cookie";
    public final static String HEADER_GET_COOKIE            = "Get-Cookie";
    public final static String HEADER_CACHE_CONTROL         = "Cache-Control";
    public final static String HEADER_CONNECTION            = "Connection";
    public final static String HEADER_AUTHORIZATION         = "Authorization";
    public final static String HEADER_HOST                  = "host";
    public final static String HEADER_X_FORWARDED_FOR       = "X-FORWARDED-FOR";
    public final static String HEADER_PROXY_CLIENT_IP       = "Proxy-Client-IP";
    public final static String HEADER_WL_PROXY_CLIENT_IP    = "WL-Proxy-Client-IP";
    public final static String HEADER_HTTP_CLIENT_IP        = "HTTP_CLIENT_IP";
    public final static String HEADER_HTTP_X_FORWARDED_FOR  = "HTTP_X_FORWARDED_FOR";
    public final static String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public final static String HEADER_HTTP_UNKNOW_ADDR      = "unknown";
    ///////////////////////////////////////////////////////////////////////////////////////
    /**
     * MIME: text
     */
    public final static String CONTENT_TYPE_HTML            = "text/html; charset=utf-8";
    public final static String CONTENT_TYPE_PLAIN           = "text/plain; charset=utf-8";
    public final static String CONTENT_TYPE_XML             = "text/xml; charset=utf-8";
    public final static String CONTENT_TYPE_WWW_FORM        = "application/x-www-form-urlencoded; charset=utf-8";
    public final static String CONTENT_TYPE_JSON            = "application/json; charset=utf-8";
    public final static String CONTENT_TYPE_SOAP            = "application/soap+xml; charset=utf-8";
    public final static String CONTENT_TYPE_M3U8            = "application/vnd.apple.mpegurl";
    /**
     * MIME: media
     */
    public final static String CONTENT_TYPE_OCT_STREAM      = "application/octet-stream";
    public final static String CONTENT_TYPE_AVI             = "video/x-msvideo";
    public final static String CONTENT_TYPE_MP4             = "video/mp4";
    public final static String CONTENT_TYPE_JPEG            = "image/jpeg";
    /**
     * MIME: compressed file
     */
    public final static String CONTENT_TYPE_GZIP            = "application/gzip";
    public final static String CONTENT_TYPE_X_GZIP          = "application/x-gzip";
    public final static String CONTENT_TYPE_X_GTAR          = "application/x-gtar";
    public final static String CONTENT_TYPE_X_TGZ           = "application/x-tgz";
    /**
     * Miscellaneous
     */
    public final static String WILDCARD_CHARACTER           = "*";
    ///////////////////////////////////////////////////////////////////////////////////////
    public static Map<String, String> parseQueryString(
            String queryString)    
    {
        String[] pairs = queryString.split("&");
        if(null == pairs)
        {
            return null;
        }

        Map<String, String> map = new HashMap<String, String>(); 

        for(String pair : pairs)
        {
            int pos = pair.indexOf("=");
            if(-1 == pos || // "=" isn't found 
                0 == pos)   // no parameter's name
            {
                continue;
            }
            
            String[] param = new String[2];
            param[0] = pair.substring(0, pos);
            param[1] = pair.substring(pos + 1);
            
            map.put(param[0], (null == param[1] ? "" : param[1]));
        }
        
        return map;
    }
    ///////////////////////////////////////////////////////////////////////////////////////
    public static Map<String, String> parseHeader(
            String headerString)    
    {
        String[] pairs = headerString.split(HEADER_EOL);
        if(null == pairs)
        {
            return null;
        }

        Map<String, String> map = new HashMap<String, String>(); 

        for(int idx = 1; idx < pairs.length; idx++)
        {
            String pair = pairs[idx];
            
            int pos = pair.indexOf(":");
            if(-1 == pos || // "=" isn't found 
                0 == pos)   // no parameter's name
            {
                continue;
            }
            
            String[] param = new String[2];
            param[0] = pair.substring(0, pos);
            param[1] = pair.substring(pos + 1);
            
            map.put(param[0].toUpperCase(), (null == param[1] ? "" : param[1].trim()));
        }
        
        return map;
    }
    ///////////////////////////////////////////////////////////////////////////////////////
    public static String toQueryString(
            Map<String, String> paramsMap)
    {
        StringBuilder builder = new StringBuilder();
        
        Iterator<Map.Entry<String, String>> entries =
                paramsMap.entrySet().iterator();
        while(true == entries.hasNext())
        {
            Map.Entry<String, String> entry = entries.next();
            
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            if(true == entries.hasNext())
            {
                builder.append('&');
            }
        }
        
        return builder.toString();
    }
    ///////////////////////////////////////////////////////////////////////////////////////
    public static class HttpsTrustManager implements X509TrustManager 
    {
        private static TrustManager[] trustManagers;
        private static final X509Certificate[] acceptedIssuers = new X509Certificate[]{};

        @Override
        public void checkClientTrusted(
                X509Certificate[] x509Certificates, String s)
        throws java.security.cert.CertificateException 
        {
        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] x509Certificates, String s)
        throws java.security.cert.CertificateException 
        {
        }

        public boolean isClientTrusted(X509Certificate[] chain) 
        {
            return true;
        }


        public boolean isServerTrusted(X509Certificate[] chain) 
        {
            return true;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() 
        {
            return acceptedIssuers;
        }

        public static void allowAllSSL() 
        {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() 
            {
                @Override
                public boolean verify(String host, SSLSession sslSession) 
                {
                    return true;
                }
            });

            SSLContext context = null;
            if (trustManagers == null) 
            {
                trustManagers = new TrustManager[]{ new HttpsTrustManager() };
            }

            try 
            {
                context = SSLContext.getInstance("TLS");
                context.init(null, trustManagers, new SecureRandom());
            } 
            catch (NoSuchAlgorithmException | KeyManagementException e) 
            {
                log.error(e.getMessage(), e);
            }

            HttpsURLConnection.setDefaultSSLSocketFactory(
                context != null ? context.getSocketFactory() : null);
        }
    }
}

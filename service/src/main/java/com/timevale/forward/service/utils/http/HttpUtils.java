package com.timevale.forward.service.utils.http;

import com.timevale.forward.service.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @auther: yuhua
 * @date: 2025/9/12 11:05
 * @description:
 */
@Slf4j
public class HttpUtils {

    private static final String TLS_1_2 = "TLS";

    private static String getTls12() {
        return TLS_1_2;
    }

    private static boolean check(X509Certificate[] arg0, String arg1) {
        if (StringUtils.isBlank(TLS_1_2)) {
            return false;
        }
        return true;
    }

    private HttpUtils() {
    }

    /**
     * 传参封装为具体的实体类
     *
     * @param url
     * @param params
     * @return
     */
    public static String doPostObject(String url, Object params) {
        return doPost(url, JsonUtils.toJson(params));
    }

    /**
     * 传参封装为Map
     *
     * @param url
     * @param params
     * @return
     */
    public static String doPostMap(String url, Map<String, Object> params) {
        return doPost(url, JsonUtils.toJson(params));
    }

    public static String doPutMap(String url, Map<String, Object> params) {
        return doPut(url, JsonUtils.toJson(params));
    }

    public static String doDeleteMap(String url, Map<String, Object> params) {
        return doDelete(url, JsonUtils.toJson(params));
    }

    public static String doPost(String url, String params) {
//        CurlUtils.curlPost(url, params)
        CloseableHttpClient httpClient = getCloseableHttpClient();
        if (httpClient == null) {
            throw new RuntimeException("create http client error");
        }
        CloseableHttpResponse httpResponse = null;
        String result = null;

        // 创建httpPost远程连接实例
        HttpPost httpPost = new HttpPost(url);
        // 配置请求参数实例
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(35000)// 设置连接请求超时时间
                .setSocketTimeout(60000)// 设置读取数据连接超时时间
                .build();
        // 为httpPost实例设置配置
        httpPost.setConfig(requestConfig);
        // 设置请求头
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(params, StandardCharsets.UTF_8));
        try {
            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpPost);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            result = EntityUtils.toString(entity);
        } catch (IOException e) {
            log.error("doPost error", e);
        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    log.error("doPost error", e);
                }
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("doPost error", e);
            }
        }
        return result;
    }

    public static String doPostMultipart(String url, Map<String, Object> params, MultipartFile file) {
        CloseableHttpClient httpClient = getCloseableHttpClient();
        if (httpClient == null) {
            throw new RuntimeException("create http client error");
        }
        CloseableHttpResponse httpResponse = null;
        String result = null;

        // 创建httpPost远程连接实例
        HttpPost httpPost = new HttpPost(url);
        // 配置请求参数实例
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(35000)// 设置连接请求超时时间
                .setSocketTimeout(60000)// 设置读取数据连接超时时间
                .build();
        // 为httpPost实例设置配置
        httpPost.setConfig(requestConfig);

        try {
            // 构建multipart请求体
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            
            // 添加文件部分
            if (file != null && !file.isEmpty()) {
                builder.addPart("file", new ByteArrayBody(file.getBytes(), file.getOriginalFilename()));
            }
            
            // 添加其他参数部分
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (entry.getValue() != null) {
                        builder.addPart(entry.getKey(), new StringBody(String.valueOf(entry.getValue()), 
                                ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8)));
                    }
                }
            }
            
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            
            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpPost);
            // 从响应对象中获取响应内容
            result = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            log.error("doPostMultipart error", e);
        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    log.error("doPostMultipart error", e);
                }
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("doPostMultipart error", e);
            }
        }
        return result;
    }

    public static String doPut(String url, String params) {
//        CurlUtils.curlPost(url, params)
        CloseableHttpClient httpClient = getCloseableHttpClient();
        if (httpClient == null) {
            throw new RuntimeException("create http client error");
        }
        CloseableHttpResponse httpResponse = null;
        String result = null;

        // 创建httpPost远程连接实例
        HttpPut httpPut = new HttpPut(url);
        // 配置请求参数实例
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(35000)// 设置连接请求超时时间
                .setSocketTimeout(60000)// 设置读取数据连接超时时间
                .build();
        // 为httpPost实例设置配置
        httpPut.setConfig(requestConfig);
        // 设置请求头
        httpPut.addHeader("Content-Type", "application/json");
        httpPut.setEntity(new StringEntity(params, StandardCharsets.UTF_8));
        try {
            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpPut);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            result = EntityUtils.toString(entity);
        } catch (IOException e) {
            log.error("doPut error", e);
        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    log.error("doPut error", e);
                }
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("doPut error", e);
            }
        }
        return result;
    }

    public static String doDelete(String url, String params) {
//        CurlUtils.curlPost(url, params)
        CloseableHttpClient httpClient = getCloseableHttpClient();
        if (httpClient == null) {
            throw new RuntimeException("create http client error");
        }
        CloseableHttpResponse httpResponse = null;
        String result = null;

        // 创建httpPost远程连接实例
        HttpDelete httpDelete = new HttpDelete(url);
        // 配置请求参数实例
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(35000)// 设置连接请求超时时间
                .setSocketTimeout(60000)// 设置读取数据连接超时时间
                .build();
        // 为httpPost实例设置配置
        httpDelete.setConfig(requestConfig);
        // 设置请求头
        httpDelete.addHeader("Content-Type", "application/json");
        try {
            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpDelete);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            result = EntityUtils.toString(entity);
        } catch (IOException e) {
            log.error("doDelete error", e);
        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    log.error("doDelete error", e);
                }
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("doDelete error", e);
            }
        }
        return result;
    }

    public static String doGet(String url, Map<String, Object> params) throws URISyntaxException, IOException {
        return doGet(url, params, null);
    }

    public static String doGet(String url, Map<String, Object> params, Map<String, String> headers) throws URISyntaxException, IOException {
//        CurlUtils.curlGet(url, params)
        CloseableHttpClient httpClient = getCloseableHttpClient();
        if (httpClient == null) {
            throw new RuntimeException("create http client error");
        }
        CloseableHttpResponse httpResponse;
        String result;
        URI uri;
        try {
            if (MapUtils.isNotEmpty(params)) {
                List<NameValuePair> pairs = new ArrayList<>();
                params.forEach((k, v) -> {
                    NameValuePair pair = new BasicNameValuePair(k, String.valueOf(v));
                    pairs.add(pair);
                });
                uri = new URIBuilder(url).setParameters(pairs).build();
            } else {
                uri = new URIBuilder(url).build();
            }
            HttpGet httpGet = new HttpGet(uri);
            httpResponse = httpClient.execute(httpGet);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            result = EntityUtils.toString(entity);
        } finally {
            // 关闭资源
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("doGet error", e);
            }
        }
        return result;
    }

    private static CloseableHttpClient getCloseableHttpClient() {
        CloseableHttpClient httpClient;
        try {
            SSLContext ctx = SSLContext.getInstance(getTls12());
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
                    boolean checkResult = check(arg0, arg1);
                    if (!checkResult) {
                        throw new IllegalArgumentException("checkClientTrusted error");
                    }
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
                    boolean checkResult = check(arg0, arg1);
                    if (!checkResult) {
                        throw new IllegalArgumentException("checkServerTrusted error");
                    }
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE);
            httpClient = HttpClients.custom()
                    .setSSLSocketFactory(socketFactory)
                    .build();
        } catch (NoSuchAlgorithmException e) {
            log.error("doGet create client error", e);
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            log.error("doGet create client error", e);
            throw new RuntimeException(e);
        }
        return httpClient;
    }

}

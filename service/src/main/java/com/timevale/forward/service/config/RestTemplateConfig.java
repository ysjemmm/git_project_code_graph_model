package com.timevale.forward.service.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RestTemplate 配置类
 * 配置连接池和字符编码，提升HTTP请求性能
 *
 * @author dijiu
 * @date 2025-10-10
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 1. 配置连接池
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);          // 最大连接数
        connectionManager.setDefaultMaxPerRoute(50); // 每个路由最大连接数

        // 2. 配置超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)           // 连接超时 5秒
                .setSocketTimeout(10000)           // 读取超时 10秒
                .setConnectionRequestTimeout(3000) // 从连接池获取连接超时 3秒
                .build();

        // 3. 创建 HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 4. 创建请求工厂
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // 5. 创建 RestTemplate
        RestTemplate restTemplate = new RestTemplate(factory);

        // 6. 配置字符编码为 UTF-8（只配置一次）
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) converter)
                        .setDefaultCharset(StandardCharsets.UTF_8);
            }
        }

        return restTemplate;
    }
}

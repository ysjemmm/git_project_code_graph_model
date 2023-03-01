package com.timevale.forward.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author yuankai
 * @date 2021/10/14 16:22
 */
@Configuration
public class ApiConfig {

    @Bean
    public RestTemplate restTemplate() {
        OkHttp3ClientHttpRequestFactory simpleClientHttpRequestFactory = new OkHttp3ClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setReadTimeout(20000);
        simpleClientHttpRequestFactory.setWriteTimeout(20000);
        simpleClientHttpRequestFactory.setConnectTimeout(20000);

        RestTemplate restTemplate = new RestTemplate(simpleClientHttpRequestFactory);

        // 配置UTF-8，防止中文乱码
        List<HttpMessageConverter<?>> httpMessageConverters = restTemplate.getMessageConverters();
        httpMessageConverters.forEach(httpMessageConverter -> {
            if (httpMessageConverter instanceof StringHttpMessageConverter) {
                StringHttpMessageConverter messageConverter = (StringHttpMessageConverter) httpMessageConverter;
                messageConverter.setDefaultCharset(StandardCharsets.UTF_8);
            }
        });
        return restTemplate;
    }
}

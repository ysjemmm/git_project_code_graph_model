package com.timevale.forward.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

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
        return new RestTemplate(simpleClientHttpRequestFactory);
    }
}

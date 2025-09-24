package com.timevale.forward.service.utils;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.service.utils.http.HttpUtils;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @auther: yuhua
 * @date: 2025/9/12 11:01
 * @description: HTTP工具类
 */
@Slf4j
public class HttpUtil {

    private HttpUtil() {

    }

    public static Map<String, Object> getParams(Object params) {
        if (params == null) {
            return new HashMap<>();
        }
        return JSON.parseObject(JSON.toJSONString(params)).getInnerMap();
    }

    public static String doGet(String url, Object params) {
        Map<String, Object> paramsMap = getParams(params);
        try {
            return HttpUtils.doGet(url, paramsMap);
        } catch (URISyntaxException | IOException e) {
            log.error("invoke case-captain error, url:{},params:{},error:{}", url, JSON.toJSONString(params), e.getMessage());
            throw new BaseBizRuntimeException("http invoke case-server error");
        }
    }

    public static String doPost(String url, Object params) {
        Map<String, Object> paramsMap = getParams(params);
        log.info("invoke case-captain, url:{},params:{}", url, JSON.toJSONString(params));

        try {
            return HttpUtils.doPostMap(url, paramsMap);
        } catch (Exception e) {
            throw new BaseBizRuntimeException("http invoke case-server error");
        }
    }

    public static String doPostMultipart(String url, Map<String, Object> params, MultipartFile file) {
        log.info("invoke case-captain, url:{},params:{}", url, JSON.toJSONString(params));

        try {
            return HttpUtils.doPostMultipart(url, params, file);
        } catch (Exception e) {
            throw new BaseBizRuntimeException("http invoke case-server error");
        }
    }

    public static String doPut(String url, Object params) {
        Map<String, Object> paramsMap = getParams(params);
        log.info("invoke case-captain, url:{},params:{}", url, JSON.toJSONString(params));

        try {
            return HttpUtils.doPutMap(url, paramsMap);
        } catch (Exception e) {
            throw new BaseBizRuntimeException("http invoke case-server error");
        }
    }

    public static String doDelete(String url, Object params) {
        Map<String, Object> paramsMap = getParams(params);
        log.info("invoke case-captain, url:{},params:{}", url, JSON.toJSONString(params));

        try {
            return HttpUtils.doDeleteMap(url, paramsMap);
        } catch (Exception e) {
            throw new BaseBizRuntimeException("http invoke case-server error");
        }
    }
}

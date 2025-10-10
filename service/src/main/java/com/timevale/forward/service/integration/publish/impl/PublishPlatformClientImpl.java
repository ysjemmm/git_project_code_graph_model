package com.timevale.forward.service.integration.publish.impl;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.dal.dto.DevopsProjectDTO;
import com.timevale.forward.dal.dto.PublishPlanResultDTO;
import com.timevale.forward.facade.api.query.PublishPlanQueryList;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author xingyun
 * @date 2022/5/12 10:57
 */
@Slf4j
@Component
public class PublishPlatformClientImpl implements PublishPlatformClient {

    @Resource
    private RestTemplate restTemplate;

    // 提取公共的API基础URL
    @Value("${devops.api.baseUrl:http://poseidon-master.officek8s.esign.cn/api}")
    private String apiBaseUrl;

    // URL构建方法
    private String getPublishPlanUrl() {
        return apiBaseUrl + "/release_plan?";
    }

    private String getProjectUrl() {
        return apiBaseUrl + "/project/";
    }

    private String getTrainListUrl() {
        return apiBaseUrl + "/train?";
    }

    private String getTrainDetailUrl() {
        return apiBaseUrl + "/train/";
    }

    private String getTrainPublishPageUrl() {
        return apiBaseUrl + "/train/{trainId}/publishPage";
    }

    @Override
    public PublishPlanResultDTO list(PublishPlanQueryList publishPlanQueryList) {
        log.info("发布计划查询: {}", publishPlanQueryList);
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        for (HttpMessageConverter<?> messageConverter : messageConverters) {
            if (messageConverter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) messageConverter).setDefaultCharset(CharsetUtil.CHARSET_UTF_8);
            }
        }
        int pageNum = publishPlanQueryList.getPageNum();
        int pageSize = publishPlanQueryList.getPageSize();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", publishPlanQueryList.getName());
        paramMap.put("exactSearch", false);
        paramMap.put("creator", publishPlanQueryList.getCreateMan());
        paramMap.put("ids", publishPlanQueryList.getId());
        paramMap.put("appName", publishPlanQueryList.getAppName());
        paramMap.put("offset", (pageNum - 1) * pageSize);
        paramMap.put("limit", pageSize);
        String params = HttpUtil.toParams(paramMap, CharsetUtil.CHARSET_UTF_8, false);
        String publishPlanUrl = getPublishPlanUrl();
        String result = restTemplate.getForObject(publishPlanUrl + params, String.class);
        log.info("请求url :{},发布平台返回结果: {}", publishPlanUrl + params, result);
        return JSONObject.parseObject(result, PublishPlanResultDTO.class);
    }

    @Override
    public DevopsProjectDTO getProject(String projectSign) {
        log.info("[PublishPlatformClientImpl.getProject]projectSign:{}", projectSign);
        if (StrUtil.isBlank(projectSign)) {
            return null;
        }

        JSONObject jsonObject = null;
        try {
            String projectUrl = getProjectUrl();
            jsonObject = restTemplate.getForObject(projectUrl + projectSign, JSONObject.class);
            log.info("[PublishPlatformClientImpl.getProject]result:{}", jsonObject);
        } catch (RestClientException e) {
            log.error("[PublishPlatformClientImpl.getProject]发布平台查询项目信息失败，projectSign:{}", projectSign);
        }

        return Optional.ofNullable(jsonObject)
                .map(obj -> obj.getJSONObject("result"))
                .map(obj -> obj.toJavaObject(DevopsProjectDTO.class))
                .orElse(null);
    }

    @Override
    public Map<String, Object> getTrainList(Map<String, Object> params) {
        log.info("[PublishPlatformClientImpl.getTrainList]params:{}", params);

        try {
            // 构建请求参数
            String queryParams = "";
            if (params != null && !params.isEmpty()) {
                queryParams = HttpUtil.toParams(params, CharsetUtil.CHARSET_UTF_8, false);
            }

            String trainListUrl = getTrainListUrl();
            String requestUrl = trainListUrl + queryParams;
            JSONObject result = restTemplate.getForObject(requestUrl, JSONObject.class);

            log.info("[PublishPlatformClientImpl.getTrainList]url:{}, result:{}", requestUrl, result);
            // 转换为Map类型
            return result != null ? result.getInnerMap() : null;

        } catch (RestClientException e) {
            log.error("[PublishPlatformClientImpl.getTrainList]调用发布火车列表接口失败", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getTrainDetail(Integer trainId) {
        log.info("[PublishPlatformClientImpl.getTrainDetail]trainId:{}", trainId);

        if (trainId == null || trainId <= 0) {
            log.warn("[PublishPlatformClientImpl.getTrainDetail]trainId为空或无效");
            return null;
        }

        try {
            String trainDetailUrl = getTrainDetailUrl();
            String requestUrl = trainDetailUrl + trainId;
            JSONObject result = restTemplate.getForObject(requestUrl, JSONObject.class);

            log.info("[PublishPlatformClientImpl.getTrainDetail]url:{}, result:{}", requestUrl, result);
            // 转换为Map类型
            return result != null ? result.getInnerMap() : null;

        } catch (RestClientException e) {
            log.error("[PublishPlatformClientImpl.getTrainDetail]调用发布火车详情接口失败，trainId:{}", trainId, e);
            return null;
        }
    }

    /**
     * 获取发布火车-发布详情
     *
     * @param trainId 发布火车ID
     * @param params 查询参数
     * @return 发布火车-发布详情
     */
    @Override
    public Map<String, Object> getTrainPublishPage(Integer trainId, Map<String, Object> params) {
        log.info("[PublishPlatformClientImpl.getTrainPublishPage]trainId:{}, params:{}", trainId, params);

        if (trainId == null || trainId <= 0) {
            log.warn("[PublishPlatformClientImpl.getTrainPublishPage]trainId为空或无效, params:{}", params);
            return null;
        }

        try {
            // 使用UriComponentsBuilder构建URL（推荐方式）
            String trainPublishPageUrl = getTrainPublishPageUrl();
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(trainPublishPageUrl);

            // 添加查询参数
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (entry.getValue() != null) {
                        uriBuilder.queryParam(entry.getKey(), entry.getValue());
                    }
                }
            }

            String requestUrl = uriBuilder.buildAndExpand(trainId).toUriString();
            JSONObject result = restTemplate.getForObject(requestUrl, JSONObject.class);

            log.info("[PublishPlatformClientImpl.getTrainPublishPage]url:{}, result:{}", requestUrl, result);

            // 转换为Map类型并处理空值
            if (result != null) {
                return result.getInnerMap();
            } else {
                log.warn("[PublishPlatformClientImpl.getTrainPublishPage]接口返回结果为空，trainId:{}, params:{}", trainId, params);
                return new HashMap<>();
            }

        } catch (RestClientException e) {
            log.error("[PublishPlatformClientImpl.getTrainPublishPage]调用发布火车详情接口失败，trainId:{}, params:{}", trainId, params, e);
            return null;
        }
    }
}

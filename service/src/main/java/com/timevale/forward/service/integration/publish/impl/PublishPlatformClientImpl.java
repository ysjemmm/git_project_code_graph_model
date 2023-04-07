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

    @Value("${publishPlan.baseUrl:http://poseidon-master.esign.cn/api/release_plan?}")
    private String publishPlanUrl;
    @Value("${project.baseUrl:http://poseidon-master.esign.cn/api/project/}")
    private String projectUrl;

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
}

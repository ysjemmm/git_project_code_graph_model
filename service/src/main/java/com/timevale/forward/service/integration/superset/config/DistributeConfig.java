package com.timevale.forward.service.integration.superset.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author yuankai
 * @date 2021/6/16 18:07
 */
// @Configuration
@Slf4j
public class DistributeConfig {
    @Value("${distribute.databases}")
    private String databaseString;

    private Map<String, DistributeConfigVO> databases;

    @PostConstruct
    public void init() {
        log.info("获取数据分发配置:{}", databaseString);
        databases = JSON.parseObject(databaseString, new TypeReference<Map<String, DistributeConfigVO>>() {
        });
    }

    /**
     * 数据指标配置
     *
     */
    public DistributeConfigVO getDataIndicator() {
        return databases.get("csm.dataIndicator");
    }

    /**
     * 数据指标配置
     *
     */
    public DistributeConfigVO getProjectOnlineLately() {
        return databases.get("csm.projectOnlineLately");
    }

}
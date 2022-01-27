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
@Configuration
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
     * 数据指标配置 —— 产品PD,开发RD
     *
     */
    public DistributeConfigVO getDataIndicatorPD() {
        return databases.get("pd.dataIndicator");
    }

    public DistributeConfigVO getDataIndicatorRD() {
        return databases.get("rd.dataIndicator");
    }

    /**
     * 近三周计划上线项目配置,数量
     *
     */
    public DistributeConfigVO getProjectOnlineLately() {
        return databases.get("all.projectOnlineLately");
    }

    public DistributeConfigVO getProjectOnlineLatelyCount() {
        return databases.get("all.projectOnlineLatelyCount");
    }

    /**
     * 风险预警 —— 开发RD,测试QA，提测，任务逾期
     *
     */
    public DistributeConfigVO getRiskWarningRD() {
        return databases.get("rd.riskWarning");
    }

    public DistributeConfigVO getRiskWarningQA() {
        return databases.get("qa.riskWarning");
    }

    public DistributeConfigVO getRiskWarningSubmitTest() {
        return databases.get("all.riskWarningSubmitTest");
    }

    public DistributeConfigVO getRiskWarningTask() {
        return databases.get("all.riskWarningTask");
    }

    /**
     * 项目工时看板
     *
     */
    public DistributeConfigVO getProjectBoard(){return databases.get("all.projectBoard");}
}
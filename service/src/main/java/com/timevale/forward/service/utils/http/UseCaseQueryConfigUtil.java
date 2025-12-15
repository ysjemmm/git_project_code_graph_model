package com.timevale.forward.service.utils.http;

import com.timevale.forward.service.config.UseCaseQueryConfig;
import com.timevale.forward.service.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @auther: yuhua
 * @date: 2025/9/15 10:39
 * @description:
 */
@Component
public class UseCaseQueryConfigUtil {

    @Value("${test.case.url: https://testmanage.esign.cn/metersphere}")
    private String testCaseUrl;

    public static final String USE_CASE_QUERY_CONFIG = "{\n" +
            "\t\"useCaseHost\": \"https://testmanage.esign.cn/metersphere\",\n" +
            "\t\"addTestPlanModuleUrl\": \"/test-plan/module/add\",\n" +
            "\t\"addTestPlanUrl\": \"/test-plan/add\",\n" +
            "\t\"statisticsUrl\": \"/test-plan/module/statistics\",\n" +
            "\t\"queryProjectUrl\": \"/project-api/getProjectByNum\",\n" +
            "\t\"searchProjectsUrl\": \"/test-plan/module/search-projects\",\n" +
            "\t\"testplanDetailsUrl\": \"/test-plan/module/test-plan-details\",\n" +
            "\t\"checkModuleExistUrl\": \"/test-plan/module/checkModuleExist\"\n" +
            "}";

    public UseCaseQueryConfig getConfigMap() {
        UseCaseQueryConfig useCaseQueryConfig = JsonUtils.fromJson(USE_CASE_QUERY_CONFIG, UseCaseQueryConfig.class);
        useCaseQueryConfig.setUseCaseHost(testCaseUrl);
        return useCaseQueryConfig;
    }

    public String getAddTestPlanModuleUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getAddTestPlanModuleUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getAddTestPlanUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getAddTestPlanUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getTestplanDetailsUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getTestplanDetailsUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getSearchProjectsUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getSearchProjectsUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryProjectUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getQueryProjectUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getStatisticsUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getStatisticsUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getCheckModuleExistUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getCheckModuleExistUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }
}

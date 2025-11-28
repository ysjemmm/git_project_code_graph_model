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
            "\t\"addTestPlanUrl\": \"/test-plan/add\"\n" +
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
}

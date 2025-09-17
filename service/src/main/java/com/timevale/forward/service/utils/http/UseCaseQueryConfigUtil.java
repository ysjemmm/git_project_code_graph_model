package com.timevale.forward.service.utils.http;

import com.timevale.crm.sdk.common.constant.enums.EnvEnum;
import com.timevale.forward.service.config.UseCaseQueryConfig;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @auther: yuhua
 * @date: 2025/9/15 10:39
 * @description:
 */
@Component
public class UseCaseQueryConfigUtil {

    @Resource
    private EnvUtils envUtils;

    public static final String USE_CASE_QUERY_CONFIG = "{\n" +
            "\t\"useCaseHost\": \"http://test-case-platform-backend-testcase-platform-test.projectk8s.tsign.cn\",\n" +
            "\t\"queryProjectGroupListUrl\": \"/tmsdefender/queryGroup\",\n" +
            "\t\"queryCasePlatformProjectListUrl\": \"/tmsdefender/queryProjectByPage\",\n" +
            "\t\"addUseCasePlatformVersionUrl\": \"/tmsdefender/addVersion\",\n" +
            "\t\"queryVersionListUrl\": \"/tmsdefender/queryVersionList\",\n" +
            "\t\"queryTurnListUrl\": \"/tmsdefender/queryTurnList\",\n" +
            "\t\"queryDemandCaseListUrl\": \"/tmsdefender/queryDemandCaseList\",\n" +
            "\t\"queryCaseListUrl\": \"/tmsdefender/queryCaseList\",\n" +
            "\t\"queryVersionLinkCaseCountUrl\": \"/tmsdefender/queryVersionLinkCount\",\n" +
            "\t\"queryTurnTreeListUrl\": \"/tmsdefender/getNeedExecuteCase\",\n" +
            "\t\"queryTurnNameUrl\": \"/tmsdefender/queryTurnName\",\n" +
            "\t\"addTurnUrl\": \"/tmsdefender/addTurn\",\n" +
            "\t\"deleteTurnUrl\": \"/tmsdefender/deleteTurn\",\n" +
            "\t\"linkOrUnLinkDemandUrl\": \"/tmsdefender/linkOrUnLinkDemand\",\n" +
            "\t\"signCaseResultUrl\": \"/tmsdefender/signCaseResult\",\n" +
            "\t\"queryTurnProgressUrl\": \"/tmsdefender/getTurnProgress\",\n" +
            "\t\"queryCaseUrl\": \"/tmsdefender/queryCase\",\n" +
            "\t\"editCaseUrl\": \"/tmsdefender/editCase\",\n" +
            "\t\"deleteCaseUrl\": \"/tmsdefender/deleteCase\"\n" +
            "}";

    public UseCaseQueryConfig getConfigMap() {
        UseCaseQueryConfig useCaseQueryConfig = JsonUtils.fromJson(USE_CASE_QUERY_CONFIG, UseCaseQueryConfig.class);
        if (EnvEnum.PROD.equals(envUtils.getEnv())) {
            useCaseQueryConfig.setUseCaseHost("http://test-case-platform-backend.testk8s.tsign.cn");
        } else if (EnvEnum.PRE.equals(envUtils.getEnv())) {
            useCaseQueryConfig.setUseCaseHost("http://test-case-platform-backend.smlk8s.esign.cn");
        } else {
            useCaseQueryConfig.setUseCaseHost("http://test-case-platform-backend-testcase-platform-test.projectk8s.tsign.cn");
        }
        return useCaseQueryConfig;
    }

    public String getQueryProjectGroupListUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getQueryProjectGroupListUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryCasePlatformProjectListUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getQueryCasePlatformProjectListUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getAddUseCasePlatformVersionUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String res = config.getAddUseCasePlatformVersionUrl();
        String host = config.getUseCaseHost();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryVersionListUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = getConfigMap().getQueryVersionListUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryTurnListUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getQueryTurnListUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryDemandCaseListUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getQueryDemandCaseListUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryCaseListUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getQueryCaseListUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryVersionLinkCaseCountUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getQueryVersionLinkCaseCountUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryTurnTreeListUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getQueryTurnTreeListUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryTurnNameUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getQueryTurnNameUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getAddTurnUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getAddTurnUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getDeleteTurnUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getDeleteTurnUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getLinkOrUnLinkDemandUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getLinkOrUnLinkDemandUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getSignCaseResultUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getSignCaseResultUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryTurnProgressUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getQueryTurnProgressUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getQueryCaseUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getQueryCaseUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getEditCaseUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getEditCaseUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }

    public String getDeleteCaseUrl() {
        UseCaseQueryConfig config = getConfigMap();
        String host = config.getUseCaseHost();
        String res = config.getDeleteCaseUrl();
        if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(res)) {
            return host + res;
        }
        return null;
    }
}

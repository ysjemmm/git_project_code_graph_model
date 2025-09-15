package com.timevale.forward.service.config;

import lombok.Data;

import java.io.Serializable;

/**
 * @auther: yuhua
 * @date: 2025/9/15 10:39
 * @description:
 */
@Data
public class UseCaseQueryConfig implements Serializable {

    private String useCaseHost;

    private String queryProjectGroupListUrl;

    private String queryCasePlatformProjectListUrl;

    private String addUseCasePlatformVersionUrl;

    private String queryVersionListUrl;

    private String queryTurnListUrl;

    private String queryDemandCaseListUrl;

    private String queryCaseListUrl;

    private String queryVersionLinkCaseCountUrl;

    private String queryTurnTreeListUrl;
}

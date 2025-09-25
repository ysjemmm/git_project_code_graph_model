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

    private String queryTurnNameUrl;

    private String addTurnUrl;

    private String deleteTurnUrl;

    private String linkOrUnLinkDemandUrl;

    private String signCaseResultUrl;

    private String queryTurnProgressUrl;

    private String queryCaseUrl;

    private String editCaseUrl;

    private String deleteCaseUrl;

    private String queryGroupProjectVersionListUrl;

    private String queryCheckCaseOverTimeUrl;

    private String queryQueryCaseOverTimeUrl;

    private String caseImageUploadUrl;

    private String caseDocumentDeleteUrl;

    private String addCaseUrl;

    private String deleteDemandCaseListUrl;
}

package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;

import java.util.Map;

public interface UseCasePlatFormCallService {
    BaseResult queryProjectGroupList(Map<String, Object> params);

    BaseResult queryCasePlatformProjectList(Map<String, Object> params);

    BaseResult addUseCasePlatformVersion(Map<String, Object> params);

    BaseResult queryVersionList(Map<String, Object> params);

    BaseResult queryTurnList(Map<String, Object> params);

    BaseResult queryDemandCaseList(Map<String, Object> params);

    BaseResult queryCaseList(Map<String, Object> params);

    BaseResult queryVersionLinkCaseCount(Map<String, Object> params);

    BaseResult queryTurnTreeList(Map<String, Object> params);

}

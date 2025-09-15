package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.Map;

@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
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

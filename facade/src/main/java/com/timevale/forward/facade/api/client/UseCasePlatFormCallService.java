package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.Map;

@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface UseCasePlatFormCallService {
    BaseResult addTestPlanModule(Map<String, Object> params);

    BaseResult addTestPlan(Map<String, Object> params);

    BaseResult queryProject(Map<String, Object> params);

    BaseResult statistics(Map<String, Object> params);

    BaseResult searchProjects(Map<String, Object> params);

    BaseResult testplanDetails(Map<String, Object> params);

    BaseResult checkModuleExist(Map<String, Object> params);
}

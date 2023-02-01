package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectIncomeSaveReq;
import com.timevale.forward.facade.api.result.ProjectIncomeDetailVO;
import com.timevale.forward.facade.api.result.ProjectIncomeVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectIncomeService {

    BaseResult<ProjectIncomeVO> addIncome(ProjectIncomeSaveReq req);

    BaseResult<ProjectIncomeVO> updateIncome(ProjectIncomeSaveReq req);

    BaseResult<Void> deleteIncome(Long id);

    BaseResult<ProjectIncomeDetailVO> listIncome(Long projectId);

}

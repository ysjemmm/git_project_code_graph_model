package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectBudgetSaveReq;
import com.timevale.forward.facade.api.result.ProjectBudgetVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectBudgetService {

    BaseResult<List<ProjectBudgetVO>> listBudgets(Long projectId);

    BaseResult<ProjectBudgetVO> addBudget(ProjectBudgetSaveReq req);

    BaseResult<ProjectBudgetVO> updateBudget(ProjectBudgetSaveReq req);

    BaseResult<Void> deleteBudget(Long id);

}

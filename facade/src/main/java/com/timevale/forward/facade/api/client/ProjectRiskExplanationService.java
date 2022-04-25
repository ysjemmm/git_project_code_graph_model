package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectRiskExplanationQueryList;
import com.timevale.forward.facade.api.query.ProjectRiskQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskExplanationAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskModifyReq;
import com.timevale.forward.facade.api.result.ProjectRiskExplanationVO;
import com.timevale.forward.facade.api.result.ProjectRiskVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author by YangXu
 * @date 2022/04/24 15:18
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectRiskExplanationService {

    /**
     * 添加
     *
     * @param explanationAddReq 项目风险-添加请求
     */
    BaseResult<Boolean> add(ProjectRiskExplanationAddReq explanationAddReq);

    /**
     * 分页查询
     *
     * @param explanationQueryList 项目风险解释查询列表
     */
    BaseResult<PageQueryResult<ProjectRiskExplanationVO>> list(ProjectRiskExplanationQueryList explanationQueryList);

}

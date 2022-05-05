package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectRiskQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskModifyReq;
import com.timevale.forward.facade.api.result.ProjectRiskVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author by YangXu
 * @date 2022/04/24 15:18
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectRiskService {
    /**
     * 项目风险-数据同步
     */
    BaseResult<Boolean> sync();

    /**
     * 添加
     *
     * @param projectRiskAddReq 项目风险-添加请求
     */
    BaseResult<Boolean> add(ProjectRiskAddReq projectRiskAddReq);

    /**
     * 更新
     *
     * @param projectRiskModifyReq 项目风险-修改请求
     */
    BaseResult<Boolean> modify(ProjectRiskModifyReq projectRiskModifyReq);
    /**
     * 单个查询
     *
     * @param projectRiskId 项目id
     */
    BaseResult<ProjectRiskVO> get(Long projectRiskId);

    /**
     * 分页查询
     *
     * @param projectRiskQueryList 项目风险-分页查询
     */
    BaseResult<PageQueryResult<ProjectRiskVO>> list(ProjectRiskQueryList projectRiskQueryList);

}

package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.request.ProjectNodeFlowCheckReq;
import com.timevale.forward.facade.api.result.ProjectNodeFlowDetailVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectNodeFlowService {


    /**
     * 查看
     *
     * @param projectId  projectId
     * @return 详情信息
     */
    BaseResult<ProjectNodeFlowDetailVO> getFlow(Long projectId);


    /**
     * 修改
     *
     * @param projectId projectId
     * @return Boolean
     */
    BaseResult<Boolean> withdraw(Long projectId);

    /**
     * 修改
     *
     * @param projectNodeFlowCheckReq projectNodeFlowCheckReq
     * @return Integer 0:提测延期,1发布正式延期
     */
    BaseResult<Integer> nodeIsDelay(ProjectNodeFlowCheckReq projectNodeFlowCheckReq);

    /**
     * 修改
     *
     * @param projectModifyReq projectModifyReq
     * @return Boolean
     */
    BaseResult<Boolean> test(ProjectModifyReq projectModifyReq);

}

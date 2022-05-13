package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectFlowAddReq;
import com.timevale.forward.facade.api.result.ProjectFlowDetailVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectFlowService {
    /**
     * 修改
     *
     * @param projectFlowAddReq 详设评审
     * @return Boolean
     */
    BaseResult<Boolean> add(ProjectFlowAddReq projectFlowAddReq);


    /**
     * 查看
     *
     * @param pid projectId
     * @return 详情信息
     */
    BaseResult<ProjectFlowDetailVO> get(Long pid);

}

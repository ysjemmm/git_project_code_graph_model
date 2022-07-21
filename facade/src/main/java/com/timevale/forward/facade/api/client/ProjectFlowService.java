package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectFlowAddReq;
import com.timevale.forward.facade.api.request.ProjectFlowDocModifyReq;
import com.timevale.forward.facade.api.result.ProjectFlowDetailVO;
import com.timevale.forward.facade.api.result.ProjectFlowNodeVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectFlowService {
    /**
     * 新增
     *
     * @param projectFlowAddReq 详设评审
     * @return 流程id
     */
    BaseResult<String> add(ProjectFlowAddReq projectFlowAddReq);

    /**
     * 修改
     *
     * @param projectFlowDocModifyReq 修改请求
     * @return Boolean
     */
    BaseResult<Boolean> modifyDoc(ProjectFlowDocModifyReq projectFlowDocModifyReq);


    /**
     * 查看
     *
     * @param projectFlowId projectFlowId
     * @return 详情信息
     */
    BaseResult<ProjectFlowDetailVO> get(Long projectFlowId);

    /**
     * 查看项目节点流程信息
     * @param projectId
     * @return
     */
    BaseResult<List<ProjectFlowNodeVO>> getFlowNodeInfo(Long projectId);

}

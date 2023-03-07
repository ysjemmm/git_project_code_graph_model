package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.EvaluateReq;
import com.timevale.forward.facade.api.request.MemberEvaluateModifyReq;
import com.timevale.forward.facade.api.request.MemberWorkloadFillReq;
import com.timevale.forward.facade.api.request.ProjectEvaluateReq;
import com.timevale.forward.facade.api.result.ProjectEvaluateVO;
import com.timevale.forward.facade.api.result.ProjectMemberEvaluateVO;
import com.timevale.forward.facade.api.result.ProjectWorkloadChangeVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author by YangXu
 * @date 2022/06/24 09:50
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectEvaluateService {
    /**
     * 列表
     *
     * @param projectId 项目id
     * @return 列表
     */
    BaseResult<ProjectMemberEvaluateVO> memberList(Long projectId);

    /**
     * 更新成员
     *
     * @param req 请求
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> updateMemberEvaluate(MemberEvaluateModifyReq req);

    /**
     * 成员工作填充
     *
     * @param req 请求
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> memberWorkloadFill(MemberWorkloadFillReq req);

    /**
     * 工作量变更校验
     *
     * @param req 请求
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<ProjectWorkloadChangeVO> workloadChangeCheck(MemberWorkloadFillReq req);

    /**
     * 列表
     *
     * @param projectId 项目id
     * @return 列表
     */
    BaseResult<ProjectEvaluateVO> evaluateList(Long projectId);

    /**
     * 评估更新
     *
     * @param req 要求事情
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> evaluateUpdate(ProjectEvaluateReq req);

}

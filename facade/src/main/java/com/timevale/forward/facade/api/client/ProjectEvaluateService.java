package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.MemberEvaluateModifyReq;
import com.timevale.forward.facade.api.result.ProjectMemberEvaluateVO;
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
     * 更新
     *
     * @param req 要求事情
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> updateMember(MemberEvaluateModifyReq req);

    /**
     * 列表
     *
     * @param projectId 项目id
     * @return 列表
     */
    BaseResult<ProjectMemberEvaluateVO> list(Long projectId);

    /**
     * 更新
     *
     * @param req 要求事情
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> update(MemberEvaluateModifyReq req);

}

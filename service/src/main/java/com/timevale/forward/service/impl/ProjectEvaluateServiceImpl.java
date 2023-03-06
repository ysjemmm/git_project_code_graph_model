package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.ProjectEvaluateService;
import com.timevale.forward.facade.api.request.MemberEvaluateModifyReq;
import com.timevale.forward.facade.api.result.ProjectMemberEvaluateVO;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LogPoint
@RestService
public class ProjectEvaluateServiceImpl implements ProjectEvaluateService {
    @Override
    public BaseResult<ProjectMemberEvaluateVO> memberList(Long projectId) {
        return null;
    }

    @Override
    public BaseResult<Boolean> updateMember(MemberEvaluateModifyReq req) {
        return null;
    }

    @Override
    public BaseResult<ProjectMemberEvaluateVO> evaluateList(Long projectId) {
        return null;
    }

    @Override
    public BaseResult<Boolean> update(MemberEvaluateModifyReq req) {
        return null;
    }
}

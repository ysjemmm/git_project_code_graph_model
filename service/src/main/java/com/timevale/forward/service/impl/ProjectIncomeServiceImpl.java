package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.ProjectIncomeService;
import com.timevale.forward.facade.api.request.ProjectIncomeSaveReq;
import com.timevale.forward.facade.api.result.ProjectIncomeDetailVO;
import com.timevale.forward.facade.api.result.ProjectIncomeVO;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;


/**
 * @author by YangXu
 * @date 2023/02/06 16:40
 */
@Slf4j
@RestService
public class ProjectIncomeServiceImpl implements ProjectIncomeService {

    @Override
    public BaseResult<ProjectIncomeVO> addIncome(ProjectIncomeSaveReq req) {

        return null;
    }

    @Override
    public BaseResult<ProjectIncomeVO> updateIncome(ProjectIncomeSaveReq req) {
        return null;
    }

    @Override
    public BaseResult<Void> deleteIncome(Long id) {
        return null;
    }

    @Override
    public BaseResult<ProjectIncomeDetailVO> listIncome(Long projectId) {
        return null;
    }
}

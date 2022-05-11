package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.facade.api.client.ProjectFlowService;
import com.timevale.forward.facade.api.request.ProjectFlowAddReq;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProjectFlowServiceImpl implements ProjectFlowService {
    @Resource
    private FileComponent fileComponent;

    @Resource
    private FileMapper fileMapper;

    @Override
    public BaseResult<Boolean> add(ProjectFlowAddReq projectFlowAddReq) {
        return BaseResult.success(true);
    }
}

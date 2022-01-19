package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.request.TaskModifyReq;
import com.timevale.forward.facade.api.result.TaskDetailVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class TaskServiceImpl implements TaskService {

    @Resource
    private PersonComponent personComponent;

    @Override
    public BaseResult<PageQueryResult<TaskVO>> list(TaskQueryList taskQueryList) {
        PageQueryResult<TaskVO> pageQueryResult = new PageQueryResult<>();
//        PageInfo<ProjectListDO> pageInfo = new PageInfo<>(projectDO);
//        pageQueryResult.setResultList(projectVO);
//        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> add(TaskAddReq taskAddReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(TaskModifyReq taskModifyReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TaskDetailVO> get(Long taskId) {
        TaskDetailVO taskDetailVO=new TaskDetailVO();
        return BaseResult.success(taskDetailVO);
    }

    @Override
    public BaseResult<Boolean> updateStatus(Long taskId, Integer type) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> enable(Long taskId) {
        return BaseResult.success(true);
    }
}

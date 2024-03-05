package com.timevale.forward.service.controller;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.request.TaskModifyReq;
import com.timevale.forward.service.copy.TaskCopier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("data/maintain")
public class DataMaintainController {
    private final TaskMapper taskMapper;

    @PostMapping("task")
    public BaseResult<Void> updateTask(@RequestBody TaskModifyReq taskModifyReq) {
        log.info("[DataMaintainController.updateTask]req : {}", taskModifyReq);
        TaskDO taskDO = TaskCopier.INSTANCE.convert(taskModifyReq);
        taskMapper.update(taskDO);
        return BaseResult.success();
    }
}

package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.TaskTimeMapper;
import com.timevale.forward.dal.entity.TaskTimeDO;
import com.timevale.forward.service.component.TaskTimeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class TaskTimeComponentImpl implements TaskTimeComponent {

    @Resource
    private TaskTimeMapper taskTimeMapper;


    @Override
    public void updateEndDate(Long taskId, Date actualEndDate) {
        TaskTimeDO taskTimeDO = new TaskTimeDO();
        taskTimeDO.setTaskId(taskId);
        taskTimeDO.setEndDate(actualEndDate);
        taskTimeMapper.update(taskTimeDO);
    }

    @Override
    public void insert(Long taskId, Date actualStartDate, Date actualEndDate) {
        TaskTimeDO taskTimeDO = new TaskTimeDO();
        taskTimeDO.setTaskId(taskId);
        taskTimeDO.setStartDate(actualStartDate);
        taskTimeDO.setEndDate(actualEndDate);
        taskTimeMapper.insert(taskTimeDO);
    }
}

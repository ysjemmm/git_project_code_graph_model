package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.TaskTimeMapper;
import com.timevale.forward.dal.dto.TaskTimeDTO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.dal.entity.TaskTimeDO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.TaskTimeComponent;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class TaskTimeComponentImpl extends BaseDistributeClientImpl<TaskTimeDTO> implements TaskTimeComponent {

    @Resource
    private TaskTimeMapper taskTimeMapper;

    @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private PersonComponent personComponent;


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
        TaskTimeDO existTaskTimeDO = taskTimeMapper.get(taskId);
        if (existTaskTimeDO != null) {
            taskTimeMapper.update(taskTimeDO);
        } else {
            taskTimeMapper.insert(taskTimeDO);
        }

    }

    @Override
    public List<TaskTimeDTO> getUseTime(TaskDO taskDO) {
        List<String> existExecutorIds = personComponent.select(taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode())
                .stream().map(PersonDO::getUserId).collect(Collectors.toList());
        ParamHelper queryParamHelper = ParamHelper.newInstance()
                .equals("task_id", taskDO.getId().toString())
                .in("user_id", existExecutorIds);
        DistributePageQueryVO queryParams = DistributePageQueryVO.builder()
                .params(queryParamHelper.params())
                .distributeConfigVO(distributeConfig.getTaskUseTime())
                .build();
        return doGet(queryParams);
    }
}

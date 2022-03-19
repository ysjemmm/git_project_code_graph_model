package com.timevale.forward.service.component.impl;

import com.timevale.erp.message.service.result.DingTodoTaskResponseBody;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.model.enums.ImprovementMeasureStatusEnum;
import com.timevale.forward.service.component.ImprovementMeasureComponent;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.GetTodoTaskMsg;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/03/19 12:02
 */
@Component
@Slf4j
public class ImprovementMeasureComponentImpl implements ImprovementMeasureComponent {

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private DingWorkRecordClient dingWorkRecordClient;


    @Override
    public void updateTodoStatus(List<ImprovementMeasureDO> improvementMeasureDOList) {
        log.info("同步改进措施钉钉待办状态，输入参数={}",improvementMeasureDOList);

        // 筛选出，已创建待办 并且 状态为待处理的事项
        List<ImprovementMeasureDO> createdTodoList = improvementMeasureDOList.stream()
                .filter(e -> e.getTodo() && ImprovementMeasureStatusEnum.PENDING.getCode().equals(e.getStatus()) &&  e.getName().contains("待办") )
                .collect(Collectors.toList());

        if(createdTodoList.isEmpty()){
            return;
        }

        // 查询执行人的 unionId
        List<String> executorIdList = createdTodoList.stream().map(ImprovementMeasureDO::getExecutorId).collect(Collectors.toList());
        Map<String, String> unionIdMap = innerUserPersonClient.getUnionIds(executorIdList);

        // 遍历查询钉钉待办状态
        GetTodoTaskMsg getTodoTaskMsg = GetTodoTaskMsg.builder().build();
        createdTodoList.forEach(e -> {
            getTodoTaskMsg.setRecordId(e.getTodoId());
            getTodoTaskMsg.setUnionId(unionIdMap.get(e.getExecutorId()));
            DingTodoTaskResponseBody task = dingWorkRecordClient.getTask(getTodoTaskMsg);
            if(task.getDone()){
                e.setStatus(ImprovementMeasureStatusEnum.COMPLETED.getCode());
            }
        });

        // 更新状态 （暂未更新入数据区，调试使用）
        Map<Long, Integer> statusMap = createdTodoList.stream().collect(Collectors.toMap(ImprovementMeasureDO::getId, ImprovementMeasureDO::getStatus));
        improvementMeasureDOList.forEach(e -> e.setStatus(statusMap.get(e.getId())));
    }
}

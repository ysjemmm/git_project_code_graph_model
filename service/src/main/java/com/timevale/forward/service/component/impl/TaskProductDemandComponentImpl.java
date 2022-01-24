package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.TaskProductDemandCondition;
import com.timevale.forward.dal.dao.TaskProductDemandMapper;
import com.timevale.forward.dal.entity.TaskProductDemandDO;
import com.timevale.forward.service.component.TaskProductDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class TaskProductDemandComponentImpl implements TaskProductDemandComponent {

    @Resource
    private TaskProductDemandMapper taskProductDemandMapper;

    @Override
    public void batchInsert(Long taskId, List<Long> productDemandIds) {
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return;
        }
        TaskProductDemandCondition condition = TaskProductDemandCondition.builder().taskId(taskId).build();
        List<TaskProductDemandDO> exists = taskProductDemandMapper.get(condition);
        List<Long> existProductDemandIds = exists.stream().map(TaskProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        log.info("任务新增-关联产品需求,existProductDemandIds={}", existProductDemandIds);
        productDemandIds.removeAll(existProductDemandIds);
        if (!CollectionUtils.isEmpty(productDemandIds)) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            Set<Long> set = new HashSet<>(productDemandIds);
            List<TaskProductDemandDO> list = set.stream().map(i -> {
                TaskProductDemandDO taskProductDemandDO = new TaskProductDemandDO();
                taskProductDemandDO.setProductDemandId(i);
                taskProductDemandDO.setTaskId(taskId);
                taskProductDemandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
                taskProductDemandDO.setCreateManId(userInfo.getId());
                return taskProductDemandDO;
            }).collect(Collectors.toList());
            taskProductDemandMapper.batchInsert(list);
        }
    }
}

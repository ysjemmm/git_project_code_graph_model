package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.TaskProductDemandCondition;
import com.timevale.forward.dal.dao.TaskProductDemandMapper;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.TaskProductDemandDO;
import com.timevale.forward.dal.entity.TaskProductDemandUpdateDO;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.component.TaskProductDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
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

    @Override
    public void update(List<Long> taskIds, Long productDemandId) {
        TaskProductDemandUpdateDO taskProductDemandDO = new TaskProductDemandUpdateDO();
        taskProductDemandDO.setIsDeleted(true);
        taskProductDemandDO.setUpdateTaskIds(taskIds);
        taskProductDemandDO.setProductDemandId(productDemandId);
        taskProductDemandMapper.update(taskProductDemandDO);
    }

    /**
     * 任务对应的产品需求状态全部变更为已暂停/作废时时，取消关联任务
     *
     * @param productDemandId productDemandId
     */
    @Override
    public void unLinkIfProductDemandStatusAllChange(Long productDemandId) {
        TaskProductDemandCondition c = TaskProductDemandCondition.builder().productDemandId(productDemandId).build();
        List<TaskProductDemandDO> taskProductDemandDO = taskProductDemandMapper.get(c);
        if (CollectionUtils.isEmpty(taskProductDemandDO)) {
            return;
        }
        List<Long> taskIds = taskProductDemandDO.stream().map(TaskProductDemandDO::getTaskId).collect(Collectors.toList());
        log.info("产品需求productDemandId:{},关联的任务taskIds:{}", productDemandId, taskIds);
        taskIds.forEach(a -> {
            List<Integer> list = taskProductDemandMapper.linkProductDemandList(a).stream()
                    .map(ProductDemandListDO::getStatus)
                    .filter(status -> (!status.equals(TaskStatusEnum.SUSPEND.getCode())
                            && !status.equals(TaskStatusEnum.INVALID.getCode()))).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(list)) {
                //任务关联的产品需求全部作废或暂停
                log.info("需要取消关联的任务taskId:{}", a);
                update(Lists.newArrayList(a), null);
            }
        });
    }
}

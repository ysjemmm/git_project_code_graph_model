package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStageEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.component.TaskProductDemandComponent;
import com.timevale.forward.service.component.TaskTimeComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TaskCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.timevale.forward.service.constant.CommonConstant.SECONDS_PER_HOUR;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class TaskComponentImpl implements TaskComponent {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskProductDemandComponent taskProductDemandComponent;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Resource
    private TaskTimeComponent taskTimeComponent;

    @Resource
    private TaskTimeMapper taskTimeMapper;

    @Override
    public BaseResult<PageQueryResult<TaskVO>> page(TaskListCondition condition, List<Long> taskIds) {
        // 查找执行人
        if (CollectionUtils.isNotEmpty(condition.getExecutorIds())) {
            taskIds = personMapper.getMainIds(condition.getExecutorIds(), taskIds, PersonTypeEnum.TASK_EXECUTOR.getCode());
            if (CollectionUtils.isEmpty(taskIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        // 所属项目
        if (CollectionUtils.isNotEmpty(condition.getProjectIds())) {
            taskIds = taskMapper.getByProjectIds(taskIds, condition.getProjectIds());
            if (CollectionUtils.isEmpty(taskIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        //产品线
        if (CollectionUtils.isNotEmpty(condition.getProductLineIds())) {
            taskIds = taskMapper.getByProductLineIds(taskIds, condition.getProductLineIds());
            if (CollectionUtils.isEmpty(taskIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        // 查任务
        buildConditionBeforeQuery(taskIds, condition);
        PageHelper.startPage(condition.getPageNum(), condition.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<TaskDO> taskDO = taskMapper.list(condition);

        taskIds = taskDO.stream().map(TaskDO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(taskIds)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        //1.填充人员信息
        Map<Long, List<PersonDO>> executorMap = personMapper.get(taskIds, PersonTypeEnum.TASK_EXECUTOR.getCode())
                .stream().collect(Collectors.groupingBy(PersonDO::getMainId));

        List<Long> projectIds = taskMapper.getProjectIds(taskIds);
        //2.填充产品线/业务域信息
        Map<Long, List<ProjectProductLineBizDomain>> productLineMap = productLineMapper.getByProjectIds(projectIds)
                .stream().collect(Collectors.groupingBy(ProjectProductLineBizDomain::getProjectId));

        //3.填充项目信息
        Map<Long, ProjectDO> projectMap = projectMapper.getByIds(projectIds).stream()
                .collect(Collectors.toMap(ProjectDO::getId, p -> p, (v1, v2) -> v1));
        List<TaskVO> taskVO = TaskCopier.INSTANCE.convert(taskDO);
        taskVO.forEach(a -> {
            List<PersonDO> executors = executorMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(executors)) {
                String executor = executors.stream().map(PersonDO::getUserName).collect(Collectors.joining(","));
                a.setExecutor(executor);
            }
            List<ProjectProductLineBizDomain> pdls = productLineMap.get(a.getProjectId());
            if (CollectionUtils.isNotEmpty(pdls)) {
                String productLineName = pdls.stream().map(ProjectProductLineBizDomain::getProductLineName).collect(Collectors.joining(","));
                a.setProductLineName(productLineName);
                String bizDomainName = pdls.stream().map(ProjectProductLineBizDomain::getBizDomainName).collect(Collectors.joining(","));
                a.setBizDomainName(bizDomainName);
            }
            a.setStatusName(TaskStatusEnum.getTextByCode(a.getStatus()));
            a.setProjectName(projectMap.get(a.getProjectId()).getName());
            a.setPmId(projectMap.get(a.getProjectId()).getPmId());
            a.setStageName(TaskStageEnum.getTextByCode(a.getStage()));
        });
        PageQueryResult<TaskVO> pageQueryResult = new PageQueryResult<>();
        PageInfo<TaskDO> pageInfo = new PageInfo<>(taskDO);
        pageQueryResult.setResultList(taskVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);

    }

    @Override
    public void updateStatusAsProjectStatusChange(Long projectId, Integer projectStatus, Boolean enableTask) {
        log.info("项目状态改变,更新任务状态 projectId:{},projectStatus:{},enableTask:{}", projectId, projectStatus,enableTask);
        List<TaskDO> existTaskDO = taskMapper.getByProjectId(projectId);
        if (CollectionUtils.isEmpty(existTaskDO)) {
            log.info("项目状态改变,项目无任务");
            return;
        }
        TaskStatusUpdateDO taskStatusUpdateDO = new TaskStatusUpdateDO();
        taskStatusUpdateDO.setProjectId(projectId);
        List<Integer> preUpdate = Lists.newArrayList(TaskStatusEnum.WAITING.getCode(), TaskStatusEnum.PROGRESS.getCode());
        if (ProjectStatusEnum.SUSPEND.getCode().equals(projectStatus)) {
            taskStatusUpdateDO.setPreUpdate(preUpdate);
            taskStatusUpdateDO.setUpdated(TaskStatusEnum.SUSPEND.getCode());
            taskMapper.updateStatusAsProjectStatusChange(taskStatusUpdateDO);
            // 暂停,耗时表更新数据
            existTaskDO.forEach(a -> {
                taskTimeComponent.updateEndDate(a.getId(), a.getActualEndDate());
            });
        } else if (ProjectStatusEnum.INVALID.getCode().equals(projectStatus)) {
            preUpdate.add(TaskStatusEnum.SUSPEND.getCode());
            taskStatusUpdateDO.setPreUpdate(preUpdate);
            taskStatusUpdateDO.setUpdated(TaskStatusEnum.INVALID.getCode());
            taskMapper.updateStatusAsProjectStatusChange(taskStatusUpdateDO);
            //解除任务产品需求关联
            List<Long> taskIds = existTaskDO.stream().map(TaskDO::getId).collect(Collectors.toList());
            taskProductDemandComponent.update(taskIds, null);

            taskTimeMapper.delete(taskIds);

        }
        if (enableTask) {
            //开启
            existTaskDO.forEach(a -> {
                if (a.getActualStartDate() == null && a.getActualEndDate() == null) {
                    a.setStatus(TaskStatusEnum.WAITING.getCode());
                } else if (a.getActualStartDate() != null && a.getActualEndDate() == null) {
                    a.setStatus(TaskStatusEnum.PROGRESS.getCode());
                    taskTimeComponent.insert(a.getId(), new Date(), null);
                }
                taskMapper.update(a);
            });
        }
    }

    @Override
    public BigDecimal getElapsedTime(Date startTime, Date endTime) {
        Long result = elapsedTimeClient.getElapsedTime(startTime, endTime);
        BigDecimal elapsedTime = new BigDecimal(result.toString());
        BigDecimal decimal = elapsedTime.divide(new BigDecimal(SECONDS_PER_HOUR), 2, BigDecimal.ROUND_HALF_UP);
        return decimal;
    }

    private void buildConditionBeforeQuery(List<Long> taskIds, TaskListCondition condition) {
        condition.setIds(taskIds);
        condition.setPlanStartDateLeft(DateUtil.getStartOfDay(condition.getPlanStartDateLeft()));
        condition.setPlanStartDateRight(DateUtil.getEndOfDay(condition.getPlanStartDateRight()));
        condition.setPlanEndDateLeft(DateUtil.getStartOfDay(condition.getPlanEndDateLeft()));
        condition.setPlanEndDateRight(DateUtil.getEndOfDay(condition.getPlanEndDateRight()));
        condition.setActualStartDateLeft(DateUtil.getStartOfDay(condition.getActualStartDateLeft()));
        condition.setActualStartDateRight(DateUtil.getEndOfDay(condition.getActualStartDateRight()));
        condition.setActualEndDateLeft(DateUtil.getStartOfDay(condition.getActualEndDateLeft()));
        condition.setActualEndDateRight(DateUtil.getEndOfDay(condition.getActualEndDateRight()));
    }
}

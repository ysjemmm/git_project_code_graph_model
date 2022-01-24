package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectProductLineBizDomain;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TaskCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class TaskComponentImpl implements TaskComponent{

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private TaskMapper taskMapper;


    @Override
    public BaseResult<PageQueryResult<TaskVO>> page (TaskListCondition condition, List<Long> taskIds) {
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
        //产品线业务域
        if (CollectionUtils.isNotEmpty(condition.getProductLineIds())) {
            taskIds = taskMapper.getByProductLineIds(taskIds, condition.getProductLineIds());
            if (CollectionUtils.isEmpty(taskIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        // 查任务
        buildConditionBeforeQuery(taskIds,condition);
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
        Map<Long, String> projectMap = projectMapper.getByIds(projectIds).stream()
                .collect(Collectors.toMap(ProjectDO::getId, ProjectDO::getName, (v1, v2) -> v1));
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
            a.setProjectName(projectMap.get(a.getProjectId()));
        });
        PageQueryResult<TaskVO> pageQueryResult = new PageQueryResult<>();
        PageInfo<TaskDO> pageInfo = new PageInfo<>(taskDO);
        pageQueryResult.setResultList(taskVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);

    }
    private void buildConditionBeforeQuery(List<Long>taskIds,TaskListCondition condition){
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

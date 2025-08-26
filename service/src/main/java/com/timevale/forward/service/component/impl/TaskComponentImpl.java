package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.dao.TaskProductDemandMapper;
import com.timevale.forward.dal.dao.TaskTimeMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.dal.entity.TaskStatusUpdateDO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProjectStageEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.model.enums.TaskTypeEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.component.TaskProductDemandComponent;
import com.timevale.forward.service.component.TaskTimeComponent;
import com.timevale.forward.service.component.UserComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.TaskCopier;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.CreateTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.DeleteTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.UpdateTodoTaskMsg;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Resource
    private InnerUserPersonClient innerUserPersonClient;
    @Resource
    private DingWorkRecordClient dingWorkRecordClient;
    @Resource
    private PersonComponent personComponent;
    @Resource
    private UserComponent userComponent;
    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private TaskProductDemandMapper taskProductDemandMapper;

    public static final String TITLE = "您收到了一条任务：%s";

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
        List<TaskDO> taskDos = taskMapper.list(condition);

        taskIds = taskDos.stream().map(TaskDO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(taskIds)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        //1.填充人员信息
        Map<Long, List<PersonDO>> executorMap = personMapper.get(taskIds, PersonTypeEnum.TASK_EXECUTOR.getCode())
                .stream().collect(Collectors.groupingBy(PersonDO::getMainId));

        //2.填充产品线
        List<Long> productLineIds = taskDos.stream().map(TaskDO::getProductLineId).collect(Collectors.toList());
        Map<Long, String> productLineMap = productLineMapper.getByIds(productLineIds)
                .stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (v1, v2) -> v2));

        //3.填充项目信息
        List<Long> projectIds = taskMapper.getProjectIds(taskIds);
        Map<Long, ProjectDO> projectMap = projectMapper.getByIds(projectIds).stream()
                .collect(Collectors.toMap(ProjectDO::getId, p -> p, (v1, v2) -> v1));

        //4.当前登陆人是否为PMO
        boolean isPMO = userComponent.isPmo();

        Map<Long, List<ProductDemandVO>> productDemandMap = taskProductDemandMapper.linkProductDemandListByTaskIds(taskIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ProductDemandListDO::getTaskId,
                        Collectors.mapping(ProductDemandCopier.INSTANCE::convert, Collectors.toList())
                ));

        List<TaskVO> taskVO = TaskCopier.INSTANCE.convert(taskDos);
        taskVO.forEach(a -> {
            List<PersonDO> executors = executorMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(executors)) {
                String executor = executors.stream().map(PersonDO::getUserName).collect(Collectors.joining(","));
                String executorId = executors.stream().map(PersonDO::getUserId).collect(Collectors.joining(","));
                a.setExecutor(executor);
                a.setExecutorId(executorId);
            }
            ProjectDO projectDO = projectMap.get(a.getProjectId());
            a.setPmId(projectDO.getPmId());
            a.setProjectName(projectDO.getName());
            a.setPrincipal(projectDO.getPrincipal());
            a.setPrincipalId(projectDO.getPrincipalId());
            a.setOtnPrincipal(projectDO.getOtnPrincipal());
            a.setOtnPrincipalId(projectDO.getOtnPrincipalId());

            a.setProductLineName(productLineMap.get(a.getProductLineId()));
            a.setStatusName(TaskStatusEnum.getTextByCode(a.getStatus()));
            a.setStageName(ProjectStageEnum.getTextByCode(a.getStage()));
            a.setTypeName(TaskTypeEnum.getTextByCode(a.getType()));
            a.setIsPMO(isPMO);
            if(a.getPlanEndDate()==null){
                //老数据
                a.setIsDelay(false);
            }else{
                boolean isDelay = (a.getActualEndDate() == null && new Date().after(a.getPlanEndDate()))
                        || (a.getActualEndDate() != null && a.getActualEndDate().after(a.getPlanEndDate()));
                a.setIsDelay(isDelay);
            }
            a.setCategory(projectMap.get(a.getProjectId()).getCategory());

            a.setProductDemandList(productDemandMap.get(a.getId()));
        });
        PageQueryResult<TaskVO> pageQueryResult = new PageQueryResult<>();
        PageInfo<TaskDO> pageInfo = new PageInfo<>(taskDos);
        pageQueryResult.setResultList(taskVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);

    }

    @Override
    public void updateStatusAsProjectStatusChange(Long projectId, Integer projectStatus, Boolean enableTask) {
        log.info("项目状态改变,更新任务状态 projectId:{},projectStatus:{},enableTask:{}", projectId, projectStatus, enableTask);
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

            existTaskDO = existTaskDO.stream().filter(a -> (preUpdate.contains(a.getStatus()))).collect(Collectors.toList());
            log.info("项目状态改变,待执行和进行中的任务,existTaskDO:{}", existTaskDO);
            existTaskDO.forEach(a -> {
                // 暂停,耗时表更新数据
                taskTimeComponent.updateEndDate(a.getId(), new Date());
                deleteTodoTask(a.getTodoId());
            });
        } else if (ProjectStatusEnum.INVALID.getCode().equals(projectStatus)) {
            preUpdate.add(TaskStatusEnum.SUSPEND.getCode());
            taskStatusUpdateDO.setPreUpdate(preUpdate);
            taskStatusUpdateDO.setUpdated(TaskStatusEnum.INVALID.getCode());
            taskMapper.updateStatusAsProjectStatusChange(taskStatusUpdateDO);

            List<Long> taskIds = existTaskDO.stream().filter(a -> (preUpdate.contains(a.getStatus()))).map(TaskDO::getId).collect(Collectors.toList());
            log.info("项目状态改变,待执行,进行中,已暂停的任务,taskIds:{}", taskIds);
            //解除任务产品需求关联
            if (!CollectionUtils.isEmpty(taskIds)) {
                taskProductDemandComponent.update(taskIds, null);
                taskTimeMapper.delete(taskIds, null);
            }
            preUpdate.remove(TaskStatusEnum.SUSPEND.getCode());
            // 待执行,进行中任务变成作废时,需要删除钉钉待办
            existTaskDO = existTaskDO.stream().filter(a -> (preUpdate.contains(a.getStatus()))).collect(Collectors.toList());
            existTaskDO.forEach(a -> deleteTodoTask(a.getTodoId()));
        }
        if (enableTask) {
            //开启暂停的任务
            existTaskDO = existTaskDO.stream().filter(a -> (TaskStatusEnum.SUSPEND.getCode().equals(a.getStatus()))).collect(Collectors.toList());
            log.info("项目状态改变,暂停的任务,existTaskDO:{}", existTaskDO);
            existTaskDO.forEach(a -> {
                if (a.getActualStartDate() == null && a.getActualEndDate() == null) {
                    a.setStatus(TaskStatusEnum.WAITING.getCode());
                } else if (a.getActualStartDate() != null && a.getActualEndDate() == null) {
                    a.setStatus(TaskStatusEnum.PROGRESS.getCode());
                    taskTimeComponent.insert(a.getId(), new Date(), null);
                }
                if (a.getTodo()) {
                    //暂停后会删除待办,启用后新增待办
                    List<String> existExecutorIds = personComponent.select(a.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode())
                            .stream().map(PersonDO::getUserId).collect(Collectors.toList());
                    addTodoTask(a, existExecutorIds,LocalSessionUtils.getUserInfo().getId());
                }
                taskMapper.update(a);
            });
        }
    }

    @Override
    public BigDecimal getElapsedTime(Date startTime, Date endTime) {
        Long result = elapsedTimeClient.getElapsedTime(startTime, endTime);
        BigDecimal elapsedTime = new BigDecimal(result.toString());
        return elapsedTime.divide(new BigDecimal(SECONDS_PER_HOUR), 2, RoundingMode.HALF_UP);
    }

    @Override
    public void addTodoTask(TaskDO taskDO, List<String> executorIds,String account) {
        if (CollectionUtils.isEmpty(executorIds)) {
            return;
        }
        boolean containsCurrentUser = true;
        if (!executorIds.contains(account)) {
            executorIds.add(account);
            containsCurrentUser = false;
        }
        Map<String, String> map = innerUserPersonClient.getUnionIds(executorIds);
        if (map.isEmpty()) {
            log.info("新增待办时,查询用户中心所属用户无unionId");
            return;
        }
        String unionId = map.get(account);
        if (!containsCurrentUser) {
            map.remove(account);
        }
        CreateTodoTaskMsg createTodoTaskMsg = CreateTodoTaskMsg.builder()
                .title(String.format(TITLE, taskDO.getName()))
                .unionId(unionId)
                .executorIds(Lists.newArrayList(map.values()))
                .dueTime(taskDO.getPlanEndDate().getTime()).build();
        String todoId = dingWorkRecordClient.addTodoTask(createTodoTaskMsg);
        taskDO.setTodoId(todoId);
        if (StringUtils.isEmpty(todoId)) {
            log.info("新增待办异常,createTodoTaskMsg :{}", createTodoTaskMsg);
            taskDO.setTodo(false);
        }
    }

    @Override
    public void updateTodoTask(TaskDO taskDO, List<String> executorIds) {
        if (CollectionUtils.isEmpty(executorIds)) {
            return;
        }
        boolean containsCurrentUser = true;
        String id = LocalSessionUtils.getUserInfo().getId();
        if (!executorIds.contains(id)) {
            executorIds.add(id);
            containsCurrentUser = false;
        }
        Map<String, String> map = innerUserPersonClient.getUnionIds(executorIds);
        if (map.isEmpty()) {
            log.info("更新待办时,查询用户中心所属用户无unionId");
            return;
        }
        String unionId = map.get(id);
        if (!containsCurrentUser) {
            map.remove(id);
        }
        UpdateTodoTaskMsg updateTodoTaskMsg = UpdateTodoTaskMsg.builder()
                .recordId(taskDO.getTodoId())
                .title(String.format(TITLE, taskDO.getName()))
                .unionId(unionId)
                .executorIds(Lists.newArrayList(map.values()))
                .participantIds(Lists.newArrayList(map.values()))
                .done(taskDO.getActualEndDate() != null)
                .dueTime(taskDO.getPlanEndDate().getTime()).build();
        log.info("更新待办,taskDO:{},executorIds:{},updateTodoTaskMsg:{}", taskDO, executorIds, updateTodoTaskMsg);
        dingWorkRecordClient.updateTask(updateTodoTaskMsg);
    }

    @Override
    public void deleteTodoTask(String todoId) {
        String id = LocalSessionUtils.getUserInfo().getId();
        threadPoolTaskExecutor.execute(() -> {
            if (StringUtils.isEmpty(todoId)) {
                return;
            }
            Map<String, String> map = innerUserPersonClient.getUnionIds(Lists.newArrayList(id));
            if (map.isEmpty()) {
                log.info("删除待办时,查询用户中心所属用户无unionId");
                return;
            }
            DeleteTodoTaskMsg deleteTodoTaskMsg = DeleteTodoTaskMsg.builder()
                    .recordId(todoId)
                    .unionId(map.get(id))
                    .build();
            dingWorkRecordClient.deleteTask(deleteTodoTaskMsg);
            log.info("删除待办,deleteTodoTaskMsg:{}", deleteTodoTaskMsg);
        });
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
        condition.setCreateDateLeft(DateUtil.getStartOfDay(condition.getCreateDateLeft()));
        condition.setCreateDateRight(DateUtil.getEndOfDay(condition.getCreateDateRight()));
    }
}

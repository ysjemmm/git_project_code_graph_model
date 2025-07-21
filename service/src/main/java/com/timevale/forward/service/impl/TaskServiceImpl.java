package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.TaskCondition;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.condition.TaskProductDemandCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.dto.TaskTimeDTO;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.query.ProductDemandLinkTaskQueryList;
import com.timevale.forward.facade.api.query.TaskLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.TaskDetailVO;
import com.timevale.forward.facade.api.result.TaskListVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.timevale.forward.service.constant.CommonConstant.SECONDS_PER_HOUR;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@LogPoint
@RestService
public class TaskServiceImpl implements TaskService {

    @Resource
    private TaskComponent taskComponent;
    @Resource
    private InnerUserPersonClient innerUserPersonClient;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private TaskTimeMapper taskTimeMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;
    @Resource
    private TaskProductDemandComponent taskProductDemandComponent;
    @Resource
    private FileComponent fileComponent;
    @Resource
    private PersonComponent personComponent;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private TaskProductDemandMapper taskProductDemandMapper;
    @Resource
    private ProductDemandComponent productDemandComponent;
    @Resource
    private ElapsedTimeClient elapsedTimeClient;
    @Resource
    private TaskTimeComponent taskTimeComponent;
    @Resource
    private ProjectNodeMapper projectNodeMapper;
    @Resource
    private UserComponent userComponent;
    @Resource
    private ProjectEvaluateComponent evaluateComponent;
    @Resource
    private InnerProjectStatusUpdateComponent innerProjectStatusUpdateComponent;

    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private TransactionTemplate transactionTemplate;

    public static final String ON_WORK_HOUR = " 09:00:00";

    public static final String OFF_WORK_HOUR = " 18:30:00";

    @Override
    public BaseResult<PageQueryResult<TaskVO>> list(TaskQueryList taskQueryList) {

        log.info("任务列表接收参数:{}", taskQueryList);
        String currentUser = LocalSessionUtils.getUserInfo().getId();
        TaskListCondition condition = TaskCopier.INSTANCE.convert(taskQueryList);
        condition.setPageNum(taskQueryList.getPageNum());
        condition.setPageSize(taskQueryList.getPageSize());
        List<Long> taskIds = new ArrayList<>();
        //1.查找我或我的团队所属任务id
        if (AscriptionEnum.CURRENT_USER.name().equals(taskQueryList.getAscription())) {
            taskIds = personMapper.getMainIds(Lists.newArrayList(currentUser), null, PersonTypeEnum.TASK_EXECUTOR.getCode());
            if (CollectionUtils.isEmpty(taskIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }

        } else if (AscriptionEnum.TEAM.name().equals(taskQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(currentUser, true);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            taskIds = personMapper.getMainIds(allMyStaffWithSelf, null, PersonTypeEnum.TASK_EXECUTOR.getCode());
            if (CollectionUtils.isEmpty(taskIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        return taskComponent.page(condition, taskIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Long> add(TaskAddReq taskAddReq) {
        log.info("任务新增接收参数:{}", taskAddReq);

        if (taskAddReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("任务名称中请勿包含空格");
        }

        TaskDO taskDO = TaskCopier.INSTANCE.convert(taskAddReq);
        //名称查重
        checkNameExisted(taskDO);
        //阶段限制
        checkTaskStage(taskDO);
        //关联本项目产品需求
        checkProductDemandIdsByProjectLinked(taskDO);
        //填充状态
        fillStatus(taskDO);

        List<String> executorIds = taskAddReq.getExecutors().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        if (!TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())) {
            //处理待办
            sendDingTodo(taskDO, executorIds);
        }

        //入库
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        taskDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        taskDO.setCreateManId(userInfo.getId());
        taskMapper.insert(taskDO);
        //耗时表入库
        insertTaskTime(taskDO);
        // 计算任务耗时
        ProjectDO projectDO = projectMapper.get(taskDO.getProjectId());
        if (projectDO != null && projectDO.getKind() != ProjectKindEnum.PBG_BASE.getCode()) {
            calTaskTime(taskDO);
        }

        if (TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())) {
            // 更新任务耗时
            taskMapper.update(taskDO);
        }

        //附件
        fileComponent.add(taskAddReq.getFiles(), taskDO.getId(), FileTypeEnum.TASK.getCode());
        //执行人
        personComponent.add(taskAddReq.getExecutors(), taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());

        // 若执行人不在项目成员中,需新增
        personComponent.addIfNotExisted(
                taskAddReq.getExecutors(),
                taskDO.getProjectId(),
                PersonTypeEnum.PROJECT_MEMBER.getCode(),
                PersonLevelEnum.EXTENSION.getCode());

        // 积分成员同步
        evaluateComponent.syncMember(taskDO.getProjectId());

        //关联产品需求
        taskProductDemandComponent.batchInsert(taskDO.getId(), taskAddReq.getProductDemandIds());

        return BaseResult.success(taskDO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(TaskModifyReq taskModifyReq) {
        log.info("任务修改接收参数:{}", taskModifyReq);
        TaskCondition condition = TaskCondition.builder().id(taskModifyReq.getId()).build();
        TaskDO existTaskDO = taskMapper.get(condition);
        if (!TaskStatusEnum.WAITING.getCode().equals(existTaskDO.getStatus())
                && !TaskStatusEnum.PROGRESS.getCode().equals(existTaskDO.getStatus())) {
            throw new BaseBizRuntimeException("任务状态不是待执行、进行中不能编辑");
        }

        TaskDO taskDO = TaskCopier.INSTANCE.convert(taskModifyReq);

        // 若是1-N的项目，只有项目经理、1-N产研团队负责人允许修改任务的计划时间
        checkPlanTimeModify(taskDO);

        checkNameExisted(taskDO);

        processTaskTime(taskDO);

        List<String> executorIds = taskModifyReq.getExecutors().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());

        //处理待办
        sendDingTodo(taskDO, executorIds);

        // 计算任务耗时
        ProjectDO projectDO = projectMapper.get(taskDO.getProjectId());
        if (projectDO != null && projectDO.getKind() != ProjectKindEnum.PBG_BASE.getCode()) {
            calTaskTime(taskDO);
        }

        taskMapper.update(taskDO);

        fileComponent.update(taskModifyReq.getFiles(), taskDO.getId(), FileTypeEnum.TASK.getCode());

        personComponent.update(taskModifyReq.getExecutors(), taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());

        // 若执行人不在项目成员中,需新增
        Integer personLevel = Optional.ofNullable(PersonLevelEnum.getByCode(taskModifyReq.getExecutorLevel()))
                .flatMap(obj -> Optional.ofNullable(obj.getCode()))
                .orElse(PersonLevelEnum.CORE.getCode());
        personComponent.addIfNotExisted(
                taskModifyReq.getExecutors(),
                taskDO.getProjectId(),
                PersonTypeEnum.PROJECT_MEMBER.getCode(),
                personLevel);

        innerProjectStatusUpdateComponent.updateProjectDateAndStatus(taskDO.getProjectId());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TaskDetailVO> get(Long taskId) {
        log.info("任务查看接收参数:{}", taskId);
        TaskCondition condition = TaskCondition.builder().id(taskId).build();
        TaskDO taskDO = taskMapper.get(condition);
        if (taskDO == null) {
            throw new BaseBizRuntimeException("该任务不存在");
        }
        TaskDetailVO taskDetailVO = TaskCopier.INSTANCE.convert(taskDO);
        taskDetailVO.setStatusName(TaskStatusEnum.getTextByCode(taskDetailVO.getStatus()));
        taskDetailVO.setStageName(ProjectStageEnum.getTextByCode(taskDetailVO.getStage()));
        //项目
        ProjectDO projectDO = projectMapper.get(taskDO.getProjectId());
        taskDetailVO.setProjectId(projectDO.getId());
        taskDetailVO.setProjectName(projectDO.getName());
        taskDetailVO.setPmId(projectDO.getPmId());
        taskDetailVO.setProjectId(projectDO.getId());
        taskDetailVO.setPrincipal(projectDO.getPrincipal());
        taskDetailVO.setPrincipalId(projectDO.getPrincipalId());
        taskDetailVO.setOtnPrincipal(projectDO.getOtnPrincipal());
        taskDetailVO.setOtnPrincipalId(projectDO.getOtnPrincipalId());
        taskDetailVO.setProjectStatus(projectDO.getStatus());

        //产品线
        ProductLineDO productLineDO = productLineMapper.selectById(taskDO.getProductLineId());
        taskDetailVO.setProductLineVO(ProductLineCopier.INSTANCE.convert(productLineDO));

        //附件
        List<FileDO> fileDO = fileComponent.select(taskId, FileTypeEnum.TASK.getCode());
        taskDetailVO.setFiles(FileCopier.INSTANCE.transform(fileDO));

        // 执行人
        List<PersonDO> personDO = personComponent.select(taskId, PersonTypeEnum.TASK_EXECUTOR.getCode());
        taskDetailVO.setExecutors(PersonCopier.INSTANCE.transform(personDO));

        //人员耗时
        if (TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())) {
            List<TaskTimeDTO> useTime = taskTimeComponent.getUseTime(taskDO);
            taskDetailVO.setTaskTimeVO(TaskTimeCopier.INSTANCE.convert(useTime));
        }

        // 是否为PMO
        taskDetailVO.setIsPMO(userComponent.isPmo());

        return BaseResult.success(taskDetailVO);
    }

    @Override
    public BaseResult<Boolean> updateStatus(Long taskId, Integer type) {
        log.info("暂停或作废任务接收参数:taskId={},type={}", taskId, type);
        TaskCondition condition = TaskCondition.builder().id(taskId).build();
        TaskDO taskDO = taskMapper.get(condition);
        AssertUtil.notNull(taskDO, "找不到该任务");
        if (taskDO.getStatus().equals(type)) {
            // 与原本数据一致,不需要更新
            return BaseResult.success(true);
        }
        AssertUtil.checkState(!taskDO.getStatus().equals(TaskStatusEnum.INVALID.getCode()),
                "任务已作废，无法继续操作");
        transactionTemplate.execute(trans -> {
            if (TaskStatusEnum.SUSPEND.getCode().equals(type)) {
                // 暂停，耗时表更新数据
                taskTimeComponent.updateEndDate(taskDO.getId(), new Date());
            } else {
                // 作废，耗时表删除数据
                taskProductDemandComponent.update(Lists.newArrayList(taskDO.getId()), null);
                taskTimeMapper.delete(Collections.singletonList(taskDO.getId()), null);
            }
            taskDO.setStatus(type);
            taskMapper.update(taskDO);
            innerProjectStatusUpdateComponent.updateProjectDateAndStatus(taskDO.getProjectId());
            return null;
        });

        // 暂停作废都需要删除钉钉待办
        taskComponent.deleteTodoTask(taskDO.getTodoId());
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> enable(Long taskId) {
        log.info("任务开启接收参数:{}", taskId);
        TaskCondition condition = TaskCondition.builder().id(taskId).build();
        TaskDO taskDO = taskMapper.get(condition);
        if (taskDO == null) {
            throw new BaseBizRuntimeException("找不到该任务");
        }
        if (!TaskStatusEnum.SUSPEND.getCode().equals(taskDO.getStatus())) {
            throw new BaseBizRuntimeException("任务状态不是已暂停,不能修改状态");
        }
        fillStatus(taskDO);
        //耗时表入库,当任务启用后是进行中时,取当前时间作为耗时表开始时间
        if (TaskStatusEnum.PROGRESS.getCode().equals(taskDO.getStatus())) {
            taskTimeComponent.insert(taskDO.getId(), new Date(), null);
        }
        if (taskDO.getTodo()) {
            //暂停后会删除待办,启用后新增待办
            List<String> existExecutorIds = personComponent.select(taskId, PersonTypeEnum.TASK_EXECUTOR.getCode())
                    .stream().map(PersonDO::getUserId).collect(Collectors.toList());
            taskComponent.addTodoTask(taskDO, existExecutorIds, LocalSessionUtils.getUserInfo().getId());
        }
        taskMapper.update(taskDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> execute(TaskExecuteReq executeReq) {
        TaskDO taskDO = taskMapper.getById(executeReq.getId());
        AssertUtil.notNull(taskDO, "找不到该任务");
        AssertUtil.checkState(TaskStatusEnum.WAITING.getCode().equals(taskDO.getStatus()), "任务状态不是待执行,不能修改状态");

        taskDO.setStatus(TaskStatusEnum.PROGRESS.getCode());
        taskDO.setActualStartDate(executeReq.getActualStartDate());
        taskMapper.update(taskDO);

        //耗时表入库
        insertTaskTime(taskDO);
        innerProjectStatusUpdateComponent.updateProjectDateAndStatus(taskDO.getProjectId());
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> done(TaskDoneReq doneReq) {
        final Long taskId = doneReq.getId();

        TaskDO taskDO = taskMapper.getById(taskId);
        AssertUtil.notNull(taskDO, "找不到该任务");
        AssertUtil.checkState(TaskStatusEnum.PROGRESS.getCode().equals(taskDO.getStatus()), "任务状态不是进行中,不能修改状态");
        AssertUtil.checkState(taskDO.getActualStartDate().compareTo(doneReq.getActualEndDate()) <= 0, "任务实际完成时间必须大于等于实际开始时间");

        taskDO.setStatus(TaskStatusEnum.DONE.getCode());
        taskDO.setActualEndDate(doneReq.getActualEndDate());

        //更新耗时表
        taskTimeComponent.updateEndDate(taskDO.getId(), taskDO.getActualEndDate());

        // 计算任务耗时
        ProjectDO projectDO = projectMapper.get(taskDO.getProjectId());
        if (projectDO != null && projectDO.getKind() != ProjectKindEnum.PBG_BASE.getCode()) {
            calTaskTime(taskDO);
        }
        taskMapper.update(taskDO);
        //更新待办
        List<String> existExecutorIds = personComponent.select(taskId, PersonTypeEnum.TASK_EXECUTOR.getCode())
                .stream().map(PersonDO::getUserId).collect(Collectors.toList());
        if (taskDO.getTodo()) {
            taskComponent.updateTodoTask(taskDO, existExecutorIds);
        }
        innerProjectStatusUpdateComponent.updateProjectDateAndStatus(taskDO.getProjectId());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(TaskLinkProductDemandQueryList taskLinkProductDemandQueryList) {
        log.info("任务-产品需求匹配,接收参数:taskQueryList={}", taskLinkProductDemandQueryList);
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(taskLinkProductDemandQueryList);
        // 该项目下的产品需求
        List<Long> inProductDemandIds = projectProductDemandMapper.getByProjectId(condition.getProjectId())
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(inProductDemandIds)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        condition.setInProductDemandIds(inProductDemandIds);
        // 过滤掉已经被该任务关联的产品需求
        if (condition.getId() != null) {
            TaskProductDemandCondition c = TaskProductDemandCondition.builder().taskId(condition.getId()).build();
            List<Long> filterProductDemandIds = taskProductDemandMapper.get(c).stream().map(TaskProductDemandDO::getProductDemandId).collect(Collectors.toList());
            condition.setFilterProductDemandIds(filterProductDemandIds);
            condition.setId(null);
        }
        condition.setStatus(Lists.newArrayList(ProductDemandStatusEnum.INCLUDED.getCode()
                , ProductDemandStatusEnum.PJ_SUSPEND.getCode()
                , ProductDemandStatusEnum.PROGRESS.getCode()
                , ProductDemandStatusEnum.ONLINE.getCode()));
        PageHelper.startPage(taskLinkProductDemandQueryList.getPageNum(), taskLinkProductDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = productDemandComponent.list(condition);
        List<ProductDemandVO> productDemandVO = ProductDemandCopier.INSTANCE.convert(productDemandListDO);
        productDemandVO.forEach(p -> {
            p.setStatusName(ProductDemandStatusEnum.getTextByCode(p.getStatus()));
            p.setPriorityName(PriorityEnum.getTextByCode(p.getPriority()));
        });
        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);

        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkProductDemand(TaskProductDemandLinkReq taskProductDemandLinkReq) {
        log.info("关联or取消关联接收参数:taskProductDemandLinkReq={}", taskProductDemandLinkReq);
        List<Long> productDemandIds = taskProductDemandLinkReq.getProductDemandIds();
        if (LinkOrUnLinkEnum.LINK.getCode().equals(taskProductDemandLinkReq.getType())) {
            taskProductDemandComponent.batchInsert(taskProductDemandLinkReq.getTaskId(), productDemandIds);

        } else {
            taskProductDemandComponent.update(Lists.newArrayList(taskProductDemandLinkReq.getTaskId()), productDemandIds.get(0));
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> linkProductDemandList(TaskProductDemandQueryList taskProductDemandQueryList) {
        //产品需求
        PageHelper.startPage(taskProductDemandQueryList.getPageNum(), taskProductDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = taskProductDemandMapper.linkProductDemandList(taskProductDemandQueryList.getTaskId());
        List<ProductDemandVO> productDemandVO = ProductDemandCopier.INSTANCE.convert(productDemandListDO);
        productDemandVO.forEach(p -> {
            p.setStatusName(ProductDemandStatusEnum.getTextByCode(p.getStatus()));
            p.setPriorityName(PriorityEnum.getTextByCode(p.getPriority()));
        });

        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);

        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<BigDecimal> getElapsedTime(ElapsedTimeQueryReq elapsedTimeQueryReq) {
        return BaseResult.success(taskComponent.getElapsedTime(elapsedTimeQueryReq.getStartTime(), elapsedTimeQueryReq.getEndTime()));
    }

    @Override
    public BaseResult<PageQueryResult<TaskListVO>> listTask(ProductDemandLinkTaskQueryList productDemandLinkTaskQueryList) {
        // 开始分页
        PageHelper.startPage(productDemandLinkTaskQueryList.pageNum, productDemandLinkTaskQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        Long projectId = productDemandLinkTaskQueryList.getProjectId();
        Long productDemandId = productDemandLinkTaskQueryList.getProductDemandId();

        List<TaskDO> taskDOList = taskMapper.getByProductDemandId(productDemandId, projectId);
        List<TaskListVO> taskListVOList = taskDOList.stream().map(TaskCopier.INSTANCE::tansfer).collect(Collectors.toList());

        // 任务执行人
        Map<Long, List<PersonDO>> executorMap = new HashMap<>();
        List<Long> taskIdList = taskDOList.stream().map(BaseDO::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(taskIdList)) {
            List<PersonDO> executorList = personMapper.get(taskIdList, PersonTypeEnum.TASK_EXECUTOR.getCode());
            executorMap = executorList.stream().collect(Collectors.groupingBy(PersonDO::getMainId));
        }

        // 填充数据
        for (TaskListVO e : taskListVOList) {
            // 执行人
            List<PersonDO> personDOList = executorMap.get(e.getId());
            String executors = personDOList.stream().map(PersonDO::getUserName).collect(Collectors.joining(","));
            e.setExecutor(executors);

            e.setProjectId(projectId);
            e.setStatusName(TaskStatusEnum.getTextByCode(e.getStatus()));
        }

        PageInfo<TaskDO> pageInfo = new PageInfo<>(taskDOList);
        PageQueryResult<TaskListVO> pageResult = new PageQueryResult<>();
        pageResult.setResultList(taskListVOList);
        ResultUtil.fillPageInfo(pageResult, pageInfo);

        return BaseResult.success(pageResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> batchAdd(TaskBatchAddReq taskBatchAddReq) {
        log.info("任务批量新增接收参数:{}", taskBatchAddReq);
        List<TaskSimpleAddReq> taskSimples = taskBatchAddReq.getTaskSimples();
        if (CollectionUtils.isEmpty(taskSimples)) {
            return BaseResult.success(true);
        }

        boolean match = taskSimples.stream().anyMatch(a -> a.getName().contains(CommonConstant.BLANK));
        if (match) {
            throw new BaseBizRuntimeException("任务名称中请勿包含空格");
        }
        Set<String> names = taskSimples.stream().map(TaskSimpleAddReq::getName).collect(Collectors.toSet());
        if (taskSimples.size() != names.size()) {
            throw new BaseBizRuntimeException("任务名称重复,请修改后重试");
        }
        //名称查重
        List<TaskDO> taskDos = TaskCopier.INSTANCE.tansfer(taskBatchAddReq.getTaskSimples());

        checkNameExisted(taskDos);
        //阶段限制
        checkTaskStage(taskDos.get(0));

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        taskSimples.forEach(a -> threadPoolTaskExecutor.execute(() -> {
            TaskDO taskDO = TaskCopier.INSTANCE.convert(a);
            taskDO.setDesc(StringUtils.EMPTY);
            taskDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            taskDO.setCreateManId(userInfo.getId());

            //填充状态
            fillStatus(taskDO);

            List<String> executorIds = a.getExecutors().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());

            sendDingTodo(taskDO, executorIds, userInfo.getId());

            taskMapper.insert(taskDO);
            //执行人
            personComponent.add(a.getExecutors(), taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
        }));

        // 执行人
        List<PersonAddReq> executorList = taskSimples.stream()
                .flatMap(e -> e.getExecutors().stream())
                .distinct()
                .collect(Collectors.toList());

        // 若执行人不在项目成员中,需新增
        Long projectId = CollUtil.getFirst(taskDos).getProjectId();
        personComponent.addIfNotExisted(executorList, projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.EXTENSION.getCode());

        // 积分成员同步
        evaluateComponent.syncMember(projectId);

        return BaseResult.success(true);

    }

    @Override
    public BaseResult<String> getElapsedEndTime(ElapsedEndTimeQueryReq elapsedEndTimeQueryReq) {
        Date startTime = elapsedEndTimeQueryReq.getStartTime();
        BigDecimal planUseTime = elapsedEndTimeQueryReq.getPlanUseTime();
        String elaspedEndTime = elapsedTimeClient.getElapsedEndTime(startTime, planUseTime.multiply(new BigDecimal(SECONDS_PER_HOUR)).longValue());
        if (elaspedEndTime.contains(ON_WORK_HOUR)) {
            List<String> workDays = elapsedTimeClient.getHolidays(startTime, DateUtil.parseToDate(elaspedEndTime, DateFormatConst.DEFAULT_DATE_FORMAT), false);
            Optional<String> max = workDays.stream().filter(a -> !Objects.equals(a, DateUtil.getDate(elaspedEndTime))).max(String::compareTo);
            if (max.isPresent()) {
                //如果当天是ON_WORK_HOUR,则返回前一天的OFF_WORK_HOUR
                String result = max.get() + OFF_WORK_HOUR;
                log.info("接口返回时间{},最终得到时间:{}", elaspedEndTime, result);
                return BaseResult.success(result);
            }
        }
        return BaseResult.success(elaspedEndTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> transferTask(TaskTransferReq transferReq) {
        log.info("任务转移:{}", transferReq);
        List<TaskDO> taskDOList = taskMapper.getByIdList(transferReq.getIds());
        boolean matchStatus = taskDOList.stream().anyMatch(a -> TaskStatusEnum.DONE.getCode().equals(a.getStatus())
                || TaskStatusEnum.INVALID.getCode().equals(a.getStatus())
                || TaskStatusEnum.PROGRESS.getCode().equals(a.getStatus()));
        if (matchStatus) {
            throw new BaseBizRuntimeException("只能转移待执行,已暂停的任务,请修改后重试");
        }
        Map<Long, String> nameMap = taskDOList.stream().collect(Collectors.toMap(TaskDO::getId, TaskDO::getName, (v1, v2) -> v1));
        transferReq.getIds().forEach(a -> {
            TaskCondition condition = TaskCondition.builder().projectId(transferReq.getProjectId()).name(nameMap.get(a)).build();
            TaskDO existTaskDO = taskMapper.get(condition);
            if (existTaskDO != null) {
                throw new BaseBizRuntimeException("任务名称: " + nameMap.get(a) + ",已存在该项目中,同一项目任务名称不能重复");
            }
        });

        boolean matchStage = taskDOList.stream().anyMatch(a -> ProjectStageEnum.DEMAND.getCode().equals(a.getStage()));
        if (!matchTaskStage(transferReq.getProjectId()) && matchStage) {
            throw new BaseBizRuntimeException("项目无需求规划阶段,不能转移含该阶段的任务,请修改后重试");
        }

        taskMapper.updateProductLineId(transferReq.getIds(), transferReq.getProductLineId(), transferReq.getProjectId());

        // 若执行人不在项目成员中,需新增
        List<PersonAddReq> executors = personMapper.get(transferReq.getIds(), PersonTypeEnum.TASK_EXECUTOR.getCode())
                .stream().map(PersonCopier.INSTANCE::convert).collect(Collectors.toList());
        personComponent.addIfNotExisted(executors, transferReq.getProjectId(), PersonTypeEnum.PROJECT_MEMBER.getCode());

        taskProductDemandComponent.update(transferReq.getIds(), null);
        return BaseResult.success(true);
    }

    private void checkNameExisted(TaskDO taskDO) {
        TaskCondition condition = TaskCondition.builder().projectId(taskDO.getProjectId()).name(taskDO.getName()).build();
        TaskDO existTaskDO = taskMapper.get(condition);
        if (taskDO.getId() == null && existTaskDO != null) {
            // 新增
            throw new BaseBizRuntimeException("该任务名称已存在,请修改后重试");
        } else if (taskDO.getId() != null && existTaskDO != null && !taskDO.getId().equals(existTaskDO.getId())) {
            // 编辑
            throw new BaseBizRuntimeException("该任务名称已存在,请修改后重试");
        }
    }

    private void checkNameExisted(List<TaskDO> taskDos) {
        List<String> names = taskDos.stream().map(TaskDO::getName).collect(Collectors.toList());
        List<String> existNames = taskMapper.getByNameAndPid(names, taskDos.get(0).getProjectId()).stream().map(TaskDO::getName).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(existNames)) {
            throw new BaseBizRuntimeException("任务名称:" + existNames + "已存在,请修改后重试");
        }
    }


    private void checkTaskStage(TaskDO taskDO) {
        if (!matchTaskStage(taskDO.getProjectId()) && ProjectStageEnum.DEMAND.getCode().equals(taskDO.getStage())) {
            throw new BaseBizRuntimeException("项目无需求规划阶段,不能创建该阶段的任务,请修改后重试");
        }
    }

    private void checkPlanTimeModify(TaskDO newTaskDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        TaskDO taskDO = taskMapper.getById(newTaskDO.getId());
        if (!Objects.equals(taskDO.getPlanStartDate(), newTaskDO.getPlanStartDate())
                || !Objects.equals(taskDO.getPlanEndDate(), newTaskDO.getPlanEndDate())) {
            ProjectDO projectDO = projectMapper.get(taskDO.getProjectId());

            if (ProjectKindEnum.PBG_OTN.getCode().equals(projectDO.getKind())) {
                boolean authority = Objects.equals(projectDO.getPmId(), userInfo.getId())
                        || Objects.equals(projectDO.getOtnPrincipalId(), userInfo.getId());
                AssertUtil.checkState(authority, "仅项目经理、1-N产研团队负责人允许修改任务的计划时间");
            }
        }
    }

    private boolean matchTaskStage(Long projectId) {
        List<ProjectNodeDO> projectNodeDos = projectNodeMapper.get(projectId);
        List<String> sureNode = Lists.newArrayList(ProjectNodeEnum.START_PLAN.getText()
                , ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText()
                , ProjectNodeEnum.DEMAND_CONSTRUE.getText()
                , ProjectNodeEnum.DEMAND_CONSTRUE_REVERSE.getText()
                , ProjectNodeEnum.UED_AUDIT.getText());
        return projectNodeDos.stream().anyMatch(a -> sureNode.contains(a.getName()));
    }

    /**
     * 只能关联本项目下的产品需求
     *
     * @param taskDO taskDO
     */
    private void checkProductDemandIdsByProjectLinked(TaskDO taskDO) {
        List<Long> productDemandIds = taskDO.getProductDemandIds();
        if (CollUtil.isEmpty(productDemandIds)) {
            return;
        }
        List<Long> existProductDemandIds = projectProductDemandMapper.getByProjectId(taskDO.getProjectId())
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        productDemandIds.removeAll(existProductDemandIds);
        if (!CollectionUtils.isEmpty(productDemandIds)) {
            throw new BaseBizRuntimeException("产品需求id:" + productDemandIds + "没有被该项目关联,请刷新后重试");
        }
    }

    private void fillStatus(TaskDO taskDO) {
        if (taskDO.getActualStartDate() == null && taskDO.getActualEndDate() == null) {
            taskDO.setStatus(TaskStatusEnum.WAITING.getCode());
        } else if (taskDO.getActualStartDate() != null && taskDO.getActualEndDate() == null) {
            taskDO.setStatus(TaskStatusEnum.PROGRESS.getCode());
        } else {
            //需计算实际耗时
            taskDO.setStatus(TaskStatusEnum.DONE.getCode());
        }
    }

    private void insertTaskTime(TaskDO taskDO) {
        if (!TaskStatusEnum.WAITING.getCode().equals(taskDO.getStatus())) {
            taskTimeComponent.insert(taskDO.getId(), taskDO.getActualStartDate(), taskDO.getActualEndDate());
        }
    }

    private void processTaskTime(TaskDO taskDO) {
        TaskCondition condition = TaskCondition.builder().id(taskDO.getId()).build();
        TaskDO existTaskDO = taskMapper.get(condition);
        if (existTaskDO == null) {
            throw new BaseBizRuntimeException("找不到该任务");
        }
        fillStatus(taskDO);
        if (existTaskDO.getActualStartDate() != null && !existTaskDO.getActualStartDate().equals(taskDO.getActualStartDate())) {
            //实际开始时间有变动
            taskTimeMapper.delete((Lists.newArrayList(taskDO.getId())), null);
        }

        TaskTimeDO existTaskTimeDO = taskTimeMapper.get(taskDO.getId());

        if (existTaskTimeDO != null && taskDO.getActualEndDate() != null) {
            //实际开始时间无变动,当有完成时间时,更新
            taskTimeComponent.updateEndDate(taskDO.getId(), taskDO.getActualEndDate());
        } else if (existTaskTimeDO == null && taskDO.getActualStartDate() != null) {
            // 如果存在开始时间,入库一条新数据
            taskTimeComponent.insert(taskDO.getId(), taskDO.getActualStartDate(), taskDO.getActualEndDate());
        }
    }

    /**
     * 当任务暂停时,扣除暂停时间,计算任务耗时
     *
     * @param taskDO taskDO
     */
    private void calTaskTime(TaskDO taskDO) {
        if (TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())) {
            AtomicLong totalTime = new AtomicLong();
            List<TaskTimeDO> list = taskTimeMapper.list(taskDO.getId());
            if (CollectionUtils.isEmpty(list)) {
                log.info("任务耗时表找不到数据,taskId:{}", taskDO.getId());
                return;
            }
            //最后数据的完成时间
            TaskTimeDO lastTaskTimeDO = list.get(list.size() - 1);
            if (lastTaskTimeDO.getEndDate().after(lastTaskTimeDO.getStartDate())) {
                // 完成时,结束时间大于暂停时间
                for (TaskTimeDO a : list) {
                    Long result = elapsedTimeClient.getElapsedTime(a.getStartDate(), a.getEndDate());
                    totalTime.getAndAdd(result);
                }
            } else {
                for (TaskTimeDO a : list) {
                    if (a.getEndDate().before(lastTaskTimeDO.getEndDate())) {
                        Long result = elapsedTimeClient.getElapsedTime(a.getStartDate(), a.getEndDate());
                        totalTime.getAndAdd(result);
                    } else if (!a.getId().equals(lastTaskTimeDO.getId())) {
                        //最后一条数据不用计算
                        Long result = elapsedTimeClient.getElapsedTime(a.getStartDate(), lastTaskTimeDO.getEndDate());
                        totalTime.getAndAdd(result);
                        //更新最后时间
                        taskTimeComponent.updateById(a.getId(), lastTaskTimeDO.getEndDate());
                        //删除不需要的数据
                        taskTimeMapper.delete(Lists.newArrayList(a.getTaskId()), a.getId());
                        break;
                    }
                }
            }
            BigDecimal elapsedTime = new BigDecimal(String.valueOf(totalTime.get()));
            BigDecimal decimal = elapsedTime.divide(new BigDecimal(SECONDS_PER_HOUR), 2, RoundingMode.HALF_UP);
            taskDO.setTaskUseTime(decimal);
        }
    }

    /**
     * 处理钉钉待办
     *
     * @param taskDO taskDO
     */
    private void sendDingTodo(TaskDO taskDO, List<String> executorIds) {
        sendDingTodo(taskDO, executorIds, LocalSessionUtils.getUserInfo().getId());
    }

    /**
     * 处理钉钉待办
     *
     * @param taskDO taskDO
     */
    private void sendDingTodo(TaskDO taskDO, List<String> executorIds, String account) {
        if (taskDO.getId() == null) {
            //新增
            if (taskDO.getTodo()) {
//            //钉钉待办
                taskComponent.addTodoTask(taskDO, executorIds, account);
            }
        } else {
            TaskCondition condition = TaskCondition.builder().id(taskDO.getId()).build();
            TaskDO existTaskDO = taskMapper.get(condition);
            log.info("编辑时,处理钉钉待办,existTaskDO:{}", existTaskDO);
            taskDO.setTodoId(existTaskDO.getTodoId());
            if (taskDO.getTodo() && StringUtils.isEmpty(existTaskDO.getTodoId())
                    && !TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())) {
//            //编辑时,状态为待执行,进行中时才能新增待办
                taskComponent.addTodoTask(taskDO, executorIds, account);
            } else {
                boolean executorChanged = false;
                List<String> existExecutorIds = personComponent.select(existTaskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode())
                        .stream().map(PersonDO::getUserId).collect(Collectors.toList());
                if (existExecutorIds.size() != executorIds.size()) {
                    executorChanged = true;
                } else {
                    List<String> tmpExecutorIds = new ArrayList<>(executorIds);
                    tmpExecutorIds.removeAll(existExecutorIds);
                    if (!tmpExecutorIds.isEmpty()) {
                        //执行人数量不变,改变了人员
                        executorChanged = true;
                    }
                }
                //1.计划结束时间有变,2.执行人有变,3.任务完成发送待办,4.任务名称改变
                boolean needUpdate = !StringUtils.isEmpty(existTaskDO.getTodoId())
                        &&
                        ((!taskDO.getPlanEndDate().equals(existTaskDO.getPlanEndDate()))
                                || executorChanged || taskDO.getActualEndDate() != null
                                || !taskDO.getName().equals(existTaskDO.getName()));
                if (needUpdate) {
                    taskComponent.updateTodoTask(taskDO, executorIds);
                }
            }

        }
    }

}

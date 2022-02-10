package com.timevale.forward.service.impl;

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
import com.timevale.forward.facade.api.query.TaskLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.TaskDetailVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.CreateTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.DeleteTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.UpdateTodoTaskMsg;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.TaskDoneMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.timevale.forward.service.constant.CommonConstant.SECONDS_PER_HOUR;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
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
    private DingWorkRecordClient dingWorkRecordClient;

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
    MessageEventPublisher messageEventPublisher;

    public static final String PRIVATE_CLOUD = "私有云";

    public static final String TITLE = "您收到了一条任务：%s";

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
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(currentUser);
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
    public BaseResult<Boolean> add(TaskAddReq taskAddReq) {
        log.info("任务新增接收参数:{}", taskAddReq);
        TaskDO taskDO = TaskCopier.INSTANCE.convert(taskAddReq);
        //名称查重
        checkNameExisted(taskDO);
        //关联本项目产品需求
        checkProductDemandIdsByProjectLinked(taskDO);
        //检查开始日期
        checkPlanDate(taskDO);
        //填充状态
        fillStatus(taskDO);

        List<String> executorIds = taskAddReq.getExecutors().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        if (!TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())) {
            //处理待办
            sendDingTodo(taskDO, executorIds);
        }
        //耗时表入库
        insertTaskTime(taskDO);
        // 计算任务耗时
        calTaskTime(taskDO);
        //入库
        taskMapper.insert(taskDO);
        //附件
        fileComponent.add(taskAddReq.getFiles(), taskDO.getId(), FileTypeEnum.TASK.getCode());
        //执行人
        personComponent.add(taskAddReq.getExecutors(), taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
        //关联产品需求
        taskProductDemandComponent.batchInsert(taskDO.getId(), taskAddReq.getProductDemandIds());

        sendDingMsg(taskDO, executorIds);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(TaskModifyReq taskModifyReq) {
        log.info("任务修改接收参数:{}", taskModifyReq);
        TaskDO taskDO = TaskCopier.INSTANCE.convert(taskModifyReq);

        checkNameExisted(taskDO);

        checkPlanDate(taskDO);

        processTaskTime(taskDO);

        List<String> executorIds = taskModifyReq.getExecutors().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());

        //处理待办
        sendDingTodo(taskDO, executorIds);

        // 计算任务耗时
        calTaskTime(taskDO);

        taskMapper.update(taskDO);

        fileComponent.update(taskModifyReq.getFiles(), taskDO.getId(), FileTypeEnum.TASK.getCode());

        personComponent.update(taskModifyReq.getExecutors(), taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());

        sendDingMsg(taskDO, executorIds);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TaskDetailVO> get(Long taskId) {
        log.info("任务查看接收参数:{}", taskId);
        TaskCondition condition = TaskCondition.builder().id(taskId).build();
        TaskDO taskDO = taskMapper.get(condition);
        if (taskDO == null) {
            throw new BaseBizRuntimeException("找不到该任务");
        }
        TaskDetailVO taskDetailVO = TaskCopier.INSTANCE.convert(taskDO);
        taskDetailVO.setStatusName(TaskStatusEnum.getTextByCode(taskDetailVO.getStatus()));
        taskDetailVO.setStageName(TaskStageEnum.getTextByCode(taskDetailVO.getStage()));
        //项目
        ProjectDO projectDO = projectMapper.get(taskDO.getProjectId());
        taskDetailVO.setProjectName(projectDO.getName());
        taskDetailVO.setPmId(projectDO.getPmId());

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
        return BaseResult.success(taskDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(Long taskId, Integer type) {
        log.info("暂停或作废任务接收参数:taskId={},type={}", taskId, type);
        TaskCondition condition = TaskCondition.builder().id(taskId).build();
        TaskDO taskDO = taskMapper.get(condition);
        if (taskDO == null) {
            throw new BaseBizRuntimeException("找不到该任务");
        }
        if (!TaskStatusEnum.WAITING.getCode().equals(taskDO.getStatus())
                && !TaskStatusEnum.PROGRESS.getCode().equals(taskDO.getStatus())) {
            throw new BaseBizRuntimeException("任务状态不是待执行、进行中不能修改状态");
        }
        taskDO.setStatus(type);
        if (TaskStatusEnum.SUSPEND.getCode().equals(type)) {
            // 暂停,耗时表更新数据
            taskTimeComponent.updateEndDate(taskDO.getId(), new Date());

        } else {
            if (TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())) {
                throw new BaseBizRuntimeException("任务状态已完成,不能修改状态");
            }
            taskProductDemandComponent.update(Lists.newArrayList(taskDO.getId()), null);
            taskTimeMapper.delete(Lists.newArrayList(taskDO.getId()));
        }
        // 删除钉钉待办
        deleteTodoTask(taskDO);
        taskDO.setTodo(false);
        taskDO.setTodoId(null);
        taskMapper.update(taskDO);
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
            addTodoTask(taskDO, existExecutorIds);
        }
        taskMapper.update(taskDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> execute(Long taskId) {
        log.info("任务执行接收参数:{}", taskId);
        TaskCondition condition = TaskCondition.builder().id(taskId).build();
        TaskDO taskDO = taskMapper.get(condition);
        if (taskDO == null) {
            throw new BaseBizRuntimeException("找不到该任务");
        }
        if (!TaskStatusEnum.WAITING.getCode().equals(taskDO.getStatus())) {
            throw new BaseBizRuntimeException("任务状态不是待执行,不能修改状态");
        }
        taskDO.setStatus(TaskStatusEnum.PROGRESS.getCode());
        taskDO.setActualStartDate(new Date());
        taskMapper.update(taskDO);

        //耗时表入库
        insertTaskTime(taskDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> done(Long taskId) {
        log.info("任务完成接收参数:{}", taskId);
        TaskCondition condition = TaskCondition.builder().id(taskId).build();
        TaskDO taskDO = taskMapper.get(condition);
        if (taskDO == null) {
            throw new BaseBizRuntimeException("找不到该任务");
        }
        if (!TaskStatusEnum.PROGRESS.getCode().equals(taskDO.getStatus())) {
            throw new BaseBizRuntimeException("任务状态不是进行中,不能修改状态");
        }
        taskDO.setStatus(TaskStatusEnum.DONE.getCode());
        taskDO.setActualEndDate(new Date());

        //更新耗时表
        taskTimeComponent.updateEndDate(taskDO.getId(), taskDO.getActualEndDate());

        // 计算任务耗时
        calTaskTime(taskDO);
        taskMapper.update(taskDO);
        //更新待办
        List<String> existExecutorIds = personComponent.select(taskId, PersonTypeEnum.TASK_EXECUTOR.getCode())
                .stream().map(PersonDO::getUserId).collect(Collectors.toList());
        if (taskDO.getTodo()) {
            updateTodoTask(taskDO, existExecutorIds);
        }
        sendDingMsg(taskDO, existExecutorIds);
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

    /**
     * 名称重复
     *
     * @param taskDO
     */
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

    /**
     * 只能关联本项目下的产品需求
     *
     * @param taskDO taskDO
     */
    private void checkProductDemandIdsByProjectLinked(TaskDO taskDO) {
        List<Long> existProductDemandIds = projectProductDemandMapper.getByProjectId(taskDO.getProjectId())
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        List<Long> productDemandIds = taskDO.getProductDemandIds();
        productDemandIds.removeAll(existProductDemandIds);
        if (!CollectionUtils.isEmpty(productDemandIds)) {
            throw new BaseBizRuntimeException("产品需求id:" + productDemandIds + "没有被该项目关联,请刷新后重试");
        }
    }

    private void checkPlanDate(TaskDO taskDO) {
        ProjectProductLineBizDomain bizDomain = productLineMapper.getById(taskDO.getProductLineId());
        if (!PRIVATE_CLOUD.equals(bizDomain.getBizDomainName())
                && taskDO.getPlanUseTime().compareTo(BigDecimal.valueOf(16)) > 0) {
            //除私有云业务域外,计划时间不能超过16h
            throw new BaseBizRuntimeException("除私有云业务域外,计划时间不能超过16小时");
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
            taskTimeMapper.delete((Lists.newArrayList(taskDO.getId())));
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
        if(TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())){
            AtomicLong totalTime = new AtomicLong();
            List<TaskTimeDO> list = taskTimeMapper.list(taskDO.getId());
            if (CollectionUtils.isEmpty(list)) {
                log.info("任务耗时表找不到数据,taskId:{}", taskDO.getId());
                return;
            }
            list.forEach(a -> {
                Long result = elapsedTimeClient.getElapsedTime(a.getStartDate(), a.getEndDate());
                totalTime.getAndAdd(result);
            });
            BigDecimal elapsedTime = new BigDecimal(String.valueOf(totalTime.get()));
            BigDecimal decimal = elapsedTime.divide(new BigDecimal(SECONDS_PER_HOUR), 2, BigDecimal.ROUND_HALF_UP);
            taskDO.setTaskUseTime(decimal);
        }
    }

    /**
     * 处理钉钉待办
     *
     * @param taskDO taskDO
     */
    private void sendDingTodo(TaskDO taskDO, List<String> executorIds) {
        if (taskDO.getId() == null) {
            //新增
            if (taskDO.getTodo()) {
//            //钉钉待办
                addTodoTask(taskDO, executorIds);
            }
        } else {
            TaskCondition condition = TaskCondition.builder().id(taskDO.getId()).build();
            TaskDO existTaskDO = taskMapper.get(condition);
            log.info("编辑时,发送钉钉待办,existTaskDO:{}", existTaskDO);
            if (taskDO.getTodo() && StringUtils.isEmpty(existTaskDO.getTodoId())) {
//            //编辑时需发送待办
                addTodoTask(taskDO, executorIds);
            } else {
                List<String> existExecutorIds = personComponent.select(existTaskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode())
                        .stream().map(PersonDO::getUserId).collect(Collectors.toList());
                List<String> tmpExecutorIds = new ArrayList<>(executorIds);
                tmpExecutorIds.removeAll(existExecutorIds);
                //1.计划结束时间有变,2.执行人有变,3.任务完成发送待办
                boolean needUpdate = !StringUtils.isEmpty(existTaskDO.getTodoId())
                        &&
                        ((!taskDO.getPlanEndDate().equals(existTaskDO.getPlanEndDate()))
                                || tmpExecutorIds.size() != 0
                                || taskDO.getActualEndDate() != null);
                if (needUpdate) {
                    updateTodoTask(taskDO, executorIds);
                }
            }

        }
    }

    /**
     * 新增待办
     *
     * @param taskDO      taskDO
     * @param executorIds executorIds
     */
    private void addTodoTask(TaskDO taskDO, List<String> executorIds) {
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
        String unionId = map.get(id);
        if (!containsCurrentUser) {
            map.remove(id);
        }
        CreateTodoTaskMsg createTodoTaskMsg = CreateTodoTaskMsg.builder()
                .title(String.format(TITLE, taskDO.getName()))
                .unionId(unionId)
                .executorIds(Lists.newArrayList(map.values()))
                .dueTime(taskDO.getPlanEndDate().getTime()).build();
        String todoId = dingWorkRecordClient.addTask(createTodoTaskMsg);
        taskDO.setTodoId(todoId);
        if (StringUtils.isEmpty(todoId)) {
            log.info("新增待办异常");
            taskDO.setTodo(false);
        }
    }

    /**
     * 更新待办
     *
     * @param taskDO      taskDO
     * @param executorIds executorIds
     */
    private void updateTodoTask(TaskDO taskDO, List<String> executorIds) {
        log.info("更新待办,taskDO:{},executorIds:{}",taskDO,executorIds);
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
        String unionId = map.get(id);
        if (!containsCurrentUser) {
            map.remove(id);
        }
        UpdateTodoTaskMsg updateTodoTaskMsg = UpdateTodoTaskMsg.builder()
                .recordId(taskDO.getTodoId())
                .unionId(unionId)
                .executorIds(Lists.newArrayList(map.values()))
                .done(taskDO.getActualEndDate() != null)
                .dueTime(taskDO.getPlanEndDate().getTime()).build();
        dingWorkRecordClient.updateTask(updateTodoTaskMsg);
    }

    /**
     * 删除待办
     *
     * @param taskDO taskDO
     */
    private void deleteTodoTask(TaskDO taskDO) {
        String id = LocalSessionUtils.getUserInfo().getId();
        Map<String, String> map = innerUserPersonClient.getUnionIds(Lists.newArrayList(id));
        DeleteTodoTaskMsg deleteTodoTaskMsg = DeleteTodoTaskMsg.builder()
                .recordId(taskDO.getTodoId())
                .unionId(map.get(id))
                .build();
        dingWorkRecordClient.deleteTask(deleteTodoTaskMsg);
    }


    /**
     * 钉钉消息处理
     *
     * @param taskDO      taskDO
     * @param executorIds 执行人
     */
    private void sendDingMsg(TaskDO taskDO, List<String> executorIds) {
//        // 通知需求接收人
        if (TaskStatusEnum.DONE.getCode().equals(taskDO.getStatus())) {
            String pmId = projectMapper.get(taskDO.getProjectId()).getPmId();
            executorIds.add(pmId);
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            String operator = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
            messageEventPublisher.publish(new TaskDoneMsgEvent(
                    this,
                    operator,
                    executorIds,
                    taskDO.getName(),
                    taskDO.getId()
            ));
        }
    }
}

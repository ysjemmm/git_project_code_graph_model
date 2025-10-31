package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.github.pagehelper.BasePageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.condition.WorkHoursRecordCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.dao.WorkHoursRecordMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.dal.entity.WorkHoursRecordDO;
import com.timevale.forward.facade.api.client.HomePageService;
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.client.WorkHoursRecordService;
import com.timevale.forward.facade.api.query.OverviewWorkHoursQueryList;
import com.timevale.forward.facade.api.query.TaskExecutorWorkHoursQueryList;
import com.timevale.forward.facade.api.query.WorkHoursRecordQueryList;
import com.timevale.forward.facade.api.request.TaskDoneReq;
import com.timevale.forward.facade.api.request.TaskExecuteReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordBatchAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordModifyReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordQueryReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.RegisterWorkHoursTaskVO;
import com.timevale.forward.facade.api.result.WorkHoursOverviewVO;
import com.timevale.forward.facade.api.result.WorkHoursProgressVO;
import com.timevale.forward.facade.api.result.WorkHoursRecordVO;
import com.timevale.forward.facade.api.result.WorkHoursRemainVO;
import com.timevale.forward.facade.api.result.WorkHoursTaskVO;
import com.timevale.forward.facade.api.result.WorkbenchesWorkHoursVO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProjectCategoryEnum;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.copy.WorkHoursRecordCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.framework.tedis.util.TedisUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.base.util.DateUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @auther: yuhua
 * @date: 2025/7/7 10:02
 * @description: 工时记录实现类
 */
@Slf4j
@LogPoint
@RestService
public class WorkHoursRecordServiceImpl implements WorkHoursRecordService {

    @Resource
    private WorkHoursRecordMapper workHoursRecordMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskService taskService;

    @Resource
    private PersonComponent personComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private SqlOrderComponent sqlOrderComponent;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Resource
    private HomePageService homePageService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // redis key前缀
    private static final String USER_KEY_PREFIX = "FORWARD:USER_TOKEN:";

    private static final List<Integer> PROJECT_STATUSES = Arrays.asList(
            ProjectStatusEnum.WAITING.getCode(),
            ProjectStatusEnum.PLANING.getCode(),
            ProjectStatusEnum.DEVING.getCode(),
            ProjectStatusEnum.TESTING.getCode(),
            ProjectStatusEnum.RELEASED.getCode()
    );

    private static final List<Integer> TASK_STATUSES = Arrays.asList(
            TaskStatusEnum.WAITING.getCode(),
            TaskStatusEnum.PROGRESS.getCode()
    );

    @Override
    public BaseResult<PageQueryResult<WorkHoursRecordVO>> list(WorkHoursRecordQueryList workHoursRecordQueryList) {
        log.info("工时记录列表,参数:{}", workHoursRecordQueryList);
        // 转换查询条件
        WorkHoursRecordCondition workHoursRecordCondition = WorkHoursRecordCopier.INSTANCE.convert(workHoursRecordQueryList);
        workHoursRecordCondition.setPageNum(workHoursRecordQueryList.getPageNum());
        workHoursRecordCondition.setPageSize(workHoursRecordQueryList.getPageSize());
        // 开始分页
        BasePageHelper.startPage(workHoursRecordCondition.getPageNum(), workHoursRecordCondition.getPageSize(), CommonConstant.REGISTER_DESC_ORDER_BY);
        // 查询
        List<WorkHoursRecordDO> workHoursRecordDOList = workHoursRecordMapper.list(workHoursRecordCondition);
        // 转换
        List<WorkHoursRecordVO> workHoursRecordVOList = WorkHoursRecordCopier.INSTANCE.convert(workHoursRecordDOList);
        // 填充分页信息
        PageInfo<WorkHoursRecordDO> pageInfo = new PageInfo<>(workHoursRecordDOList);
        PageQueryResult<WorkHoursRecordVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(workHoursRecordVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(Long workHoursRecordId) {
        log.info("工时记录删除,参数:{}", workHoursRecordId);
        WorkHoursRecordDO workHoursRecordDO = workHoursRecordMapper.getById(workHoursRecordId);
        if (workHoursRecordDO == null) {
            throw new BaseBizRuntimeException("不存在对应的工时记录");
        }
        // 登记人
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        // 只有任务执行人可删除
        List<PersonDO> personDOList = personComponent.select(workHoursRecordDO.getWorkItemId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
        List<String> executorIds = PersonCopier.INSTANCE.transform(personDOList).stream().map(PersonVO::getUserId).collect(Collectors.toList());
        if (!executorIds.contains(userInfo.getId())) {
            throw new BaseBizRuntimeException("非任务执行人不能删除该任务工时");
        }
        workHoursRecordMapper.deleteById(workHoursRecordId);

        // 更新实际开始时间和结束时间
        updateActualStartAndEndDate(workHoursRecordDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Long> add(WorkHoursRecordAddReq workTimeRecordAddReq) {
        log.info("工时记录新增,参数:{}", workTimeRecordAddReq);
        WorkHoursRecordDO workHoursRecordDO = WorkHoursRecordCopier.INSTANCE.convert(workTimeRecordAddReq);
        List<WorkHoursRecordDO> lastProgress = workHoursRecordMapper.getLastProgress(Collections.singletonList(workHoursRecordDO.getProjectId()), workHoursRecordDO.getWorkItemType(), Collections.singletonList(workHoursRecordDO.getWorkItemId()));
        // 如果集合不为空，取最大进度
        int maxProgress = lastProgress.stream()
                .mapToInt(WorkHoursRecordDO::getProgress)
                .max()
                .orElse(0);
        // 如果工时为0不登记
        if (workHoursRecordDO.getWorkHours().compareTo(BigDecimal.ZERO) == 0 && maxProgress == workHoursRecordDO.getProgress()) {
            return BaseResult.success(null);
        }
        // 登记人
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        workHoursRecordDO.setCreateMan(buildCreateMan(userInfo));
        workHoursRecordDO.setCreateManId(userInfo.getId());
        // 登记时间，保持日期部分不变
        LocalDate registrationDate = workHoursRecordDO.getRegistrationDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // 时间部分设为当前时间
        workHoursRecordDO.setRegistrationDate(Date.from(registrationDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()));
        // 检测
        saveBeforeCheckTask(workHoursRecordDO, false);
        // 入库
        workHoursRecordMapper.insert(workHoursRecordDO);
        return BaseResult.success(workHoursRecordDO.getId());
    }

    private void saveBeforeCheckTask(WorkHoursRecordDO workHoursRecordDO, boolean isBatchAdd) {
        // 查询登记工时所属任务
        if (Objects.equals(BizTypeEnum.TASK.getCode(), workHoursRecordDO.getWorkItemType())) {
            TaskDO taskDO = taskMapper.getById(workHoursRecordDO.getWorkItemId());
            if (taskDO == null) {
                throw new BaseBizRuntimeException("任务不存在");
            }
            // 执行人
            List<PersonDO> personDOList = personComponent.select(taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
            List<String> executorIds = PersonCopier.INSTANCE.transform(personDOList).stream().map(PersonVO::getUserId).collect(Collectors.toList());
            if (!executorIds.contains(workHoursRecordDO.getCreateManId())) {
                throw new BaseBizRuntimeException("非任务执行人不能登记该任务工时");
            }
            if (!isBatchAdd) {
                // 获取登记日期
                BigDecimal accumulateWorkHours = getAccumulateWorkHours(workHoursRecordDO, workHoursRecordDO.getCreateManId());

                // 检查登记日期是否已经登记满24小时工时
                if (accumulateWorkHours.add(workHoursRecordDO.getWorkHours()).compareTo(new BigDecimal(24)) > 0) {
                    throw new BaseBizRuntimeException("您的当日工时超出24小时，请重新填写");
                }
            }

            // 如果任务没有开启执行，则登记工时直接开启任务
            updateTaskStatus(workHoursRecordDO, taskDO);

            // 更新任务实际开始和结束日期
            updateActualStartAndEndDate(workHoursRecordDO);
        }
    }

    private void updateBeforeCheckTask(WorkHoursRecordDO workHoursRecordDO) {
        // 查询登记工时所属任务
        if (Objects.equals(BizTypeEnum.TASK.getCode(), workHoursRecordDO.getWorkItemType())) {
            TaskDO taskDO = taskMapper.getById(workHoursRecordDO.getWorkItemId());
            if (taskDO == null) {
                throw new BaseBizRuntimeException("任务不存在");
            }
            WorkHoursRecordDO recordDO = workHoursRecordMapper.getById(workHoursRecordDO.getId());
            if (recordDO == null) {
                throw new BaseBizRuntimeException("工时记录不存在");
            }
            // 登记人
            UserInfo userInfo = LocalSessionUtils.getUserInfo();

            // 执行人
            List<PersonDO> personDOList = personComponent.select(taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
            List<String> executorIds = PersonCopier.INSTANCE.transform(personDOList).stream().map(PersonVO::getUserId).collect(Collectors.toList());
            if (!executorIds.contains(userInfo.getId())) {
                throw new BaseBizRuntimeException("非任务执行人不能登记该任务工时");
            }

            BigDecimal accumulateWorkHours = getAccumulateWorkHours(workHoursRecordDO, userInfo.getId());

            // 检查登记日期是否已经登记满24小时工时
            if (accumulateWorkHours.subtract(recordDO.getWorkHours()).add(workHoursRecordDO.getWorkHours()).compareTo(new BigDecimal(24)) > 0) {
                throw new BaseBizRuntimeException("您的当日工时超出24小时，请重新填写");
            }
            // 检查该任务进度是否开启或者已经完成
            updateTaskStatus(workHoursRecordDO, taskDO);
        }
    }

    private BigDecimal getAccumulateWorkHours(WorkHoursRecordDO workHoursRecordDO, String userId) {
        // 获取登记日期
        Date registrationDate = workHoursRecordDO.getRegistrationDate();
        // 开始时间（00:00:00）
        LocalDateTime startTime = LocalDateTime.ofInstant(registrationDate.toInstant(), ZoneId.systemDefault()).with(LocalTime.MIN);
        // 结束时间（23:59:59.999999999）
        LocalDateTime endTime = LocalDateTime.ofInstant(registrationDate.toInstant(), ZoneId.systemDefault()).with(LocalTime.MAX);
        // 登记人登记日期已登记工时
        return workHoursRecordMapper.getDailyWorkingHours(WorkHoursRecordCondition.builder()
                        .createManId(userId)
                        .stratTime(startTime.format(DATE_TIME_FORMATTER))
                        .endTime(endTime.format(DATE_TIME_FORMATTER))
                        .build())
                .stream()
                .map(WorkHoursRecordDO::getWorkHours)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private void updateTaskStatus(WorkHoursRecordDO workHoursRecordDO, TaskDO taskDO) {
        // 如果任务没有开启执行，则登记工时直接开启任务
        LocalDate registrationDate = workHoursRecordDO.getRegistrationDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        // 获取任务开始时间
        Date actualStartDate = Date.from(registrationDate.atTime(9, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
        // 获取任务结束时间
        Date actualEndDate = Date.from(registrationDate.atTime(18, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

        if (TaskStatusEnum.WAITING.getCode().equals(taskDO.getStatus())) {
            TaskExecuteReq taskExecuteReq = new TaskExecuteReq();
            taskExecuteReq.setId(taskDO.getId());
            taskExecuteReq.setActualStartDate(actualStartDate);
            taskService.execute(taskExecuteReq);
        }
        // 如果任务进度是100，则任务直接完成
        if (TaskStatusEnum.PROGRESS.getCode().equals(taskDO.getStatus()) && workHoursRecordDO.getProgress() >= 100) {
            TaskDoneReq taskDoneReq = new TaskDoneReq();
            taskDoneReq.setId(taskDO.getId());
            taskDoneReq.setActualEndDate(actualEndDate);
            taskService.done(taskDoneReq);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(WorkHoursRecordModifyReq workHoursRecordModifyReq) {
        log.info("工时记录修改接收参数:{}", workHoursRecordModifyReq);
        WorkHoursRecordDO recordDO = workHoursRecordMapper.getById(workHoursRecordModifyReq.getId());
        if (Objects.isNull(recordDO)) {
            throw new BaseBizRuntimeException("工时记录不存在，请新增后修改");
        }
        WorkHoursRecordDO workHoursRecordDO = WorkHoursRecordCopier.INSTANCE.convert(workHoursRecordModifyReq);
        // 登记时间，保持日期部分不变
        LocalDate registrationDate = workHoursRecordDO.getRegistrationDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // 时间部分设为当前时间
        workHoursRecordDO.setRegistrationDate(Date.from(registrationDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()));
        // 检测
        updateBeforeCheckTask(workHoursRecordDO);
        workHoursRecordMapper.update(workHoursRecordDO);

        // 更新实际开始时间和实际完成时间
        updateActualStartAndEndDate(workHoursRecordDO);

        return BaseResult.success(true);
    }

    private void updateActualStartAndEndDate(WorkHoursRecordDO workHoursRecordDO) {
        List<WorkHoursRecordDO> hoursRecordDOList = workHoursRecordMapper.list(WorkHoursRecordCondition.builder()
                .workItemType(BizTypeEnum.TASK.getCode())
                .workItemId(workHoursRecordDO.getWorkItemId())
                .projectId(workHoursRecordDO.getProjectId())
                .build());

        TaskDO taskDO = taskMapper.getById(workHoursRecordDO.getWorkItemId());
        AssertUtil.notNull(taskDO, "任务不存在");
        if (CollUtil.isNotEmpty(hoursRecordDOList)) {
            // 获取hoursRecordDOList中最早的时间
            Date startTime = hoursRecordDOList.stream()
                    .map(WorkHoursRecordDO::getRegistrationDate)
                    .min(Date::compareTo)
                    .orElse(null);
            // 获取hoursRecordDOList中进度达到100的最晚的时间
            Date endTime = hoursRecordDOList.stream()
                    .filter(hours -> hours.getProgress() >= 100)
                    .map(WorkHoursRecordDO::getRegistrationDate)
                    .max(Date::compareTo)
                    .orElse(null);
            taskDO.setActualStartDate(startTime);
            taskDO.setActualEndDate(endTime);
            taskMapper.updateActualStartAndEndDate(taskDO);
        } else {
            taskDO.setActualStartDate(null);
            taskDO.setActualEndDate(null);
            taskMapper.updateActualStartAndEndDate(taskDO);
        }
    }

    @Override
    public BaseResult<WorkHoursRecordVO> get(Long workHoursRecordId) {
        log.info("工时记录查看接收参数:{}", workHoursRecordId);
        WorkHoursRecordDO workHoursRecordDO = workHoursRecordMapper.getById(workHoursRecordId);
        if (workHoursRecordDO == null) {
            throw new BaseBizRuntimeException("该工时记录不存在");
        }
        WorkHoursRecordVO workHoursRecordVO = WorkHoursRecordCopier.INSTANCE.convert(workHoursRecordDO);
        if (Objects.equals(workHoursRecordDO.getWorkItemType(), BizTypeEnum.TASK.getCode())) {
            TaskDO taskDO = taskMapper.getById(workHoursRecordDO.getWorkItemId());
            ProjectDO projectDO = projectMapper.get(taskDO.getProjectId());
            workHoursRecordVO.setProjectName(projectDO.getName());
            workHoursRecordVO.setWorkItemName(taskDO.getName());
        }
        return BaseResult.success(workHoursRecordVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> batchAdd(WorkHoursRecordBatchAddReq workHoursRecordBatchAddReq) {
        log.info("工时记录批量新增接收参数:{}", workHoursRecordBatchAddReq);
        List<WorkHoursRecordAddReq> workHoursSimples = workHoursRecordBatchAddReq.getWorkHoursSimples();
        if (CollectionUtils.isEmpty(workHoursSimples)) {
            return BaseResult.success(true);
        }

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        // 最早登记时间
        Date date = workHoursSimples.stream()
                .map(WorkHoursRecordAddReq::getRegistrationDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (Boolean.FALSE.equals(workHoursRecordBatchAddReq.getIsInner()) && Objects.nonNull(date)) {
            String registrationDate = DateUtils.format(date, "yyyy-MM-dd");
            if (Objects.isNull(TedisUtil.get(USER_KEY_PREFIX + registrationDate + ":" + userInfo.getId()))) {
                throw new BaseBizRuntimeException("token已过期，请修改登记日期为近三天工作日的日期");
            }
        }

        String createMan = buildCreateMan(userInfo);
        String createManId = userInfo.getId();

        List<Long> projectIds = new ArrayList<>();
        List<Long> workItemIds = new ArrayList<>();
        workHoursSimples.forEach(a -> {
            projectIds.add(a.getProjectId());
            workItemIds.add(a.getWorkItemId());
        });

        // 如果工时为0且进度不更新，则不处理
        List<WorkHoursRecordDO> lastProgress = workHoursRecordMapper.getLastProgress(projectIds, BizTypeEnum.TASK.getCode(), workItemIds);
        // 获取进度Map
        Map<Long, Integer> lastProgressMap = lastProgress.stream().collect(Collectors.toMap(WorkHoursRecordDO::getWorkItemId, WorkHoursRecordDO::getProgress, (v1, v2) -> v2));

        // 收集需要处理的记录
        List<WorkHoursRecordDO> recordsToInsert = new ArrayList<>();

        // 填报日期累计工时
        Map<Date, BigDecimal> dateWorkHoursMap = workHoursSimples
                .stream()
                .collect(Collectors.groupingBy(WorkHoursRecordAddReq::getRegistrationDate, Collectors.mapping(WorkHoursRecordAddReq::getWorkHours, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        for (WorkHoursRecordAddReq a : workHoursSimples) {
            // 如果工时为0不登记且进度没有更新，则不处理
            if (a.getWorkHours().compareTo(BigDecimal.ZERO) <= 0 && Objects.equals(lastProgressMap.getOrDefault(a.getWorkItemId(), 0), a.getProgress())) {
                continue;
            }
            // 转换
            WorkHoursRecordDO workHoursRecordDO = WorkHoursRecordCopier.INSTANCE.convert(a);
            workHoursRecordDO.setCreateMan(createMan);
            workHoursRecordDO.setCreateManId(createManId);
            Date doRegistrationDate = workHoursRecordDO.getRegistrationDate();
            // 登记时间，保持日期部分不变
            LocalDate registrationDate = doRegistrationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // 时间部分设为当前时间
            workHoursRecordDO.setRegistrationDate(Date.from(registrationDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()));
            // 获取登记日期
            BigDecimal accumulateWorkHours = getAccumulateWorkHours(workHoursRecordDO, workHoursRecordDO.getCreateManId());

            // 检查登记日期是否已经登记满24小时工时
            if (accumulateWorkHours.add(dateWorkHoursMap.getOrDefault(doRegistrationDate, BigDecimal.ZERO)).compareTo(new BigDecimal(24)) > 0) {
                throw new BaseBizRuntimeException(registrationDate + "工时超出24小时，请重新填写");
            }
            saveBeforeCheckTask(workHoursRecordDO, true);
            recordsToInsert.add(workHoursRecordDO);
        }


        // 批量插入所有记录（在同一个事务中）
        for (WorkHoursRecordDO workHoursRecordDO : recordsToInsert) {
            workHoursRecordMapper.insert(workHoursRecordDO);
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<WorkHoursRemainVO> remainInfo(WorkHoursRecordQueryReq workHoursRecordQueryReq) {
        WorkHoursRemainVO workHoursRemainVO = new WorkHoursRemainVO();
        // 查询工时记录所属任务
        if (Objects.equals(BizTypeEnum.TASK.getCode(), workHoursRecordQueryReq.getWorkItemType())) {
            TaskDO taskDO = taskMapper.getById(workHoursRecordQueryReq.getWorkItemId());
            BigDecimal planUseTime = taskDO.getPlanUseTime();

            if (planUseTime == null) {
                planUseTime = BigDecimal.ZERO;
            }

            List<WorkHoursRecordDO> hoursRecordDOList = workHoursRecordMapper.list(WorkHoursRecordCondition.builder()
                    .projectId(taskDO.getProjectId())
                    .workItemType(workHoursRecordQueryReq.getWorkItemType())
                    .workItemId(workHoursRecordQueryReq.getWorkItemId())
                    .build());
            // 填报总工时
            BigDecimal sum = hoursRecordDOList.stream().map(WorkHoursRecordDO::getWorkHours).reduce(BigDecimal.ZERO, BigDecimal::add);

            // 最新工时进度 - 按创建时间排序取最新一条
            Integer progress = hoursRecordDOList.stream()
                    .max(Comparator.comparing(WorkHoursRecordDO::getRegistrationDate))
                    .map(WorkHoursRecordDO::getProgress)
                    .orElse(0);

            // 剩余工时
            BigDecimal subtract = planUseTime.subtract(sum);
            // 确保不为负数
            BigDecimal safeRemainingHour = subtract.signum() > 0 ? subtract : BigDecimal.ZERO;

            workHoursRemainVO.setEstimatedHours(planUseTime);
            workHoursRemainVO.setTotalManHour(sum);
            workHoursRemainVO.setLatestProgress(progress);
            workHoursRemainVO.setRemainingManHour(safeRemainingHour);
        }
        return BaseResult.success(workHoursRemainVO);
    }

    @Override
    public BaseResult<WorkHoursProgressVO> progressInfo(WorkHoursRecordQueryReq workHoursRecordQueryReq) {
        WorkHoursProgressVO workHoursProgressVO = new WorkHoursProgressVO();

        // 安全获取 remainInfo 的 data
        BaseResult<WorkHoursRemainVO> remainResult = remainInfo(workHoursRecordQueryReq);
        WorkHoursRemainVO workHoursRemainVO = Optional.ofNullable(remainResult)
                .map(BaseResult::getData)
                .orElse(null);

        if (Objects.nonNull(workHoursRemainVO)) {
            BigDecimal totalManHour = workHoursRemainVO.getTotalManHour();
            BigDecimal remainingManHour = workHoursRemainVO.getRemainingManHour();
            BigDecimal estimatedHours = workHoursRemainVO.getEstimatedHours();

            // 防止 null 导致后续 NPE
            totalManHour = totalManHour != null ? totalManHour : BigDecimal.ZERO;
            remainingManHour = remainingManHour != null ? remainingManHour : BigDecimal.ZERO;
            estimatedHours = estimatedHours != null ? estimatedHours : BigDecimal.ZERO;

            // 已登记工时
            workHoursProgressVO.setTotalManHour(totalManHour);

            // 预估偏差 = 预估工时 -（已登记工时+剩余工时）
            BigDecimal usedAndRemaining = totalManHour.add(remainingManHour);
            BigDecimal estimateVariance = estimatedHours.subtract(usedAndRemaining);
            workHoursProgressVO.setTotalEstimateVariance(estimateVariance);

            // 总预估工时
            workHoursProgressVO.setTotalEstimatedHours(estimatedHours);

            // 工时进度：防止除以 0
            BigDecimal denominator = usedAndRemaining.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : usedAndRemaining;
            BigDecimal timeProgress = totalManHour.multiply(new BigDecimal(100))
                    .divide(denominator, 2, RoundingMode.HALF_UP);
            workHoursProgressVO.setTotalTimeProgress(timeProgress);

            // 总剩余工时
            workHoursProgressVO.setTotalRemainingHours(remainingManHour);
        }

        return BaseResult.success(workHoursProgressVO);
    }

    @Override
    public BaseResult<List<RegisterWorkHoursTaskVO>> waitRegisterTaskList(String userId, String dateStr) {
        if (StringUtils.isBlank(userId)) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            userId = userInfo.getId();
        }
        LocalDate localDate;
        if (StringUtils.isBlank(dateStr)) {
            // 字符串日期不要时间
            localDate = LocalDate.now();
        } else {
            try {
                localDate = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                throw new BaseBizRuntimeException("日期格式错误");
            }
        }

        // 1. 查询开启通知的项目列表
        // 通过项目状态和类别查询项目，并过滤出需要工时通知的项目，将其ID与名称映射为Map
        Map<Long, String> notifyProjectMap = projectMapper.getByWorkHoursNotify(PROJECT_STATUSES, ProjectCategoryEnum.PRODUCT_PROJECT.getCode(), true)
                .stream()
                .filter(e -> ProjectKindEnum.PBG_BASE.getCode().equals(e.getKind()))
                .collect(Collectors.toMap(ProjectDO::getId, ProjectDO::getName, (e1, e2) -> e1));

        // 如果没有找到开启工时通知的项目任务
        if (notifyProjectMap.isEmpty()) {
            return BaseResult.success(new ArrayList<>());
        }
        // 将通知项目的ID收集到列表中
        List<Long> projectIds = new ArrayList<>(notifyProjectMap.keySet());

        List<RegisterWorkHoursTaskVO> registerWorkHoursTaskVOS = new ArrayList<>();

        List<Long> executorTaskIds = personMapper.getMainIds(Lists.newArrayList(userId), null, PersonTypeEnum.TASK_EXECUTOR.getCode());
        // 是否存在进行中的任务
        if (executorTaskIds.isEmpty()) {
            return BaseResult.success(registerWorkHoursTaskVOS);
        }

        String date = localDate.format(DATE_FORMATTER);
        // 查询当前用户进行中的任务
        List<TaskDO> progressTaskList = taskMapper.getProgressTaskList(TaskListCondition.builder()
                .projectIds(projectIds)
                .status(TASK_STATUSES)
                .currentDate(date)
                .ids(executorTaskIds)
                .build());
        // 是否存在进行中的任务
        if (progressTaskList.isEmpty()) {
            return BaseResult.success(registerWorkHoursTaskVOS);
        }
        // 查询任务对应的项目名称
        Map<Long, String> projectMap = projectMapper.getByIds(projectIds).stream().filter(e -> PROJECT_STATUSES.contains(e.getStatus())).collect(Collectors.toMap(ProjectDO::getId, ProjectDO::getName, (v1, v2) -> v1));
        // 过滤掉无效项目
        List<TaskDO> taskDOList = progressTaskList.stream().filter(taskDO -> projectMap.containsKey(taskDO.getProjectId())).collect(Collectors.toList());
        // 任务列表
        for (TaskDO taskDO : taskDOList) {
            RegisterWorkHoursTaskVO registerWorkHoursTaskVO = new RegisterWorkHoursTaskVO();
            registerWorkHoursTaskVO.setWorkItemId(taskDO.getId());
            registerWorkHoursTaskVO.setProjectId(taskDO.getProjectId());
            registerWorkHoursTaskVO.setProjectName(projectMap.get(taskDO.getProjectId()));
            registerWorkHoursTaskVO.setName(taskDO.getName());
            registerWorkHoursTaskVO.setWorkItemType(BizTypeEnum.TASK.getCode());
            registerWorkHoursTaskVO.setDateStr(date);
            registerWorkHoursTaskVO.setStatus(taskDO.getStatus());
            registerWorkHoursTaskVO.setStatusName(TaskStatusEnum.getTextByCode(taskDO.getStatus()));
            registerWorkHoursTaskVO.setDesc(taskDO.getDesc());
            registerWorkHoursTaskVO.setEstimatedHours(taskDO.getPlanUseTime());
            registerWorkHoursTaskVOS.add(registerWorkHoursTaskVO);
        }
        return BaseResult.success(registerWorkHoursTaskVOS);
    }

    @Override
    public BaseResult<PageQueryResult<WorkbenchesWorkHoursVO>> workbenches(TaskExecutorWorkHoursQueryList query) {
        // 1. 查询项目信息
        ProjectDO projectDO = projectMapper.get(query.getProjectId());
        if (Objects.isNull(projectDO)) {
            throw new BaseBizRuntimeException("项目不存在");
        }
        ProjectVO projectVO = ProjectCopier.INSTANCE.transform(projectDO);
        Long projectId = projectVO.getId();

        // 设置默认排序字段和方式
        if (StringUtils.isBlank(query.getOrderFiled())) {
            query.setOrderCollation(1);
            query.setOrderFiled("totalHours");
        }

        // 2. 查询工时记录和项目成员
        WorkHoursRecordCondition workHoursRecordCondition = WorkHoursRecordCondition.builder()
                .projectIds(Collections.singletonList(projectId))
                .build();
        List<WorkHoursRecordDO> hoursRecordDOList = workHoursRecordMapper.list(workHoursRecordCondition);
        List<PersonDO> personDOList = personMapper.get(Collections.singleton(projectId), PersonTypeEnum.PROJECT_MEMBER.getCode());

        // 3. 时间处理
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zoneId);
        LocalDate yesterday = today.minusDays(1);
        // 上一个周五
        LocalDate lastFriday = today.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));

        // 4. 构建工时记录时间映射
        Map<Long, LocalDate> recordDateMap = hoursRecordDOList.stream()
                .collect(Collectors.toMap(WorkHoursRecordDO::getId,
                        workHoursRecordDO -> workHoursRecordDO.getRegistrationDate().toInstant().atZone(zoneId).toLocalDate()));

        // 5. 按用户分组工时记录
        Map<String, List<WorkHoursRecordDO>> memberRecordsMap = hoursRecordDOList.stream()
                .collect(Collectors.groupingBy(WorkHoursRecordDO::getCreateManId));

        // 6. 构建返回 VO
        List<WorkbenchesWorkHoursVO> memberHours = personDOList.stream()
                .map(member -> {
                    List<WorkHoursRecordDO> memberRecords = memberRecordsMap.getOrDefault(member.getUserId(), Collections.emptyList());

                    // 计算总工时、今日、昨日工时
                    BigDecimal[] hours = memberRecords.stream()
                            .collect(
                                    () -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO},
                                    (acc, workHoursRecordDO) -> {
                                        LocalDate recordDate = recordDateMap.get(workHoursRecordDO.getId());
                                        BigDecimal workHours = workHoursRecordDO.getWorkHours();

                                        acc[0] = acc[0].add(workHours);
                                        if (recordDate.equals(today)) {
                                            acc[1] = acc[1].add(workHours);
                                        } else if (recordDate.equals(yesterday)) {
                                            acc[2] = acc[2].add(workHours);
                                        } else if (recordDate.equals(lastFriday)) {
                                            acc[3] = acc[3].add(workHours);
                                        }
                                    },
                                    (a, b) -> {
                                        a[0] = a[0].add(b[0]);
                                        a[1] = a[1].add(b[1]);
                                        a[2] = a[2].add(b[2]);
                                        a[3] = a[3].add(b[3]);
                                    }
                            );

                    return getWorkbenchesWorkHoursVO(member, hours, today);
                })
                .collect(Collectors.toList());

        // 7. 分页逻辑
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // 排序
        String collation = sqlOrderComponent.buildWithoutId(query.getOrderFiled(), query.getOrderCollation());
        Comparator<WorkbenchesWorkHoursVO> comparator = buildComparator(collation);
        List<WorkbenchesWorkHoursVO> sortedList = memberHours.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        // 手动分页
        int total = sortedList.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<WorkbenchesWorkHoursVO> paginatedList = sortedList.subList(fromIndex, toIndex);

        // 构造分页对象
        PageQueryResult<WorkbenchesWorkHoursVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(paginatedList);
        pageQueryResult.setTotalItems(total);
        pageQueryResult.setCurrentPage(pageNum);
        pageQueryResult.setItemsPerPage(pageSize);
        pageQueryResult.setTotalPages((int) Math.ceil((double) total / pageSize));

        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<PageQueryResult<WorkHoursOverviewVO>> overview(OverviewWorkHoursQueryList query) {
        log.info("工时概览查询,参数:{}", query);

        // 处理日期范围
        Date startDate = query.getStartDate();
        Date endDate = query.getEndDate();
        LocalDateTime startTime = LocalDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault()).with(LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.ofInstant(endDate.toInstant(), ZoneId.systemDefault()).with(LocalTime.MAX);

        if (CollUtil.isEmpty(query.getProjectIds())) {
            List<Long> projectIds = projectMapper.list(ProjectListCondition.builder().workHoursNotify(1).status(PROJECT_STATUSES).build())
                    .stream().map(ProjectListDO::getId).collect(Collectors.toList());
            query.setProjectIds(projectIds);
        }

        // 查询工时记录
        WorkHoursRecordCondition workHoursRecordCondition = WorkHoursRecordCondition.builder()
                .projectIds(query.getProjectIds())
                .workItemType(BizTypeEnum.TASK.getCode())
                .stratTime(startTime.format(DATE_TIME_FORMATTER))
                .endTime(endTime.format(DATE_TIME_FORMATTER))
                .createManIds(query.getMemberIds())
                .build();

        List<WorkHoursRecordDO> hoursRecordDOList = workHoursRecordMapper.list(workHoursRecordCondition);

        // 获取工作日
        List<String> workDays = elapsedTimeClient.getHolidays(startDate, endDate, false);

        // 创建人id和name的Map
        Map<String, String> createManMap = buildCreateManMap(query);

        // 查询这些人的任务
        List<String> executorIds = new ArrayList<>(createManMap.keySet());
        if (executorIds.isEmpty()) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        List<TaskDO> taskDOList = taskMapper.list(TaskListCondition.builder()
                .projectIds(query.getProjectIds())
                // 需要计划开始时间和计划结束时间与选择的范围时间有交集
                .startDate(DateUtil.getStartOfDay(startDate))
                .endDate(DateUtil.getEndOfDay(endDate))
                .build());

        // 补充实际登记了的任务
        List<Long> taskIdList = taskDOList.stream().map(TaskDO::getId).collect(Collectors.toList());
        List<Long> taskIds = hoursRecordDOList.stream().map(WorkHoursRecordDO::getWorkItemId).collect(Collectors.toList());
        // 补充实际登记了但未在初始查询结果中的任务
        if (CollUtil.isNotEmpty(taskIds)) {
            // 获取不在初始任务列表中的任务ID
            List<Long> missingTaskIds = taskIds.stream()
                    .filter(taskId -> taskIdList.stream().noneMatch(id -> id.equals(taskId)))
                    .collect(Collectors.toList());

            // 如果有缺失的任务ID，则查询并添加到任务列表中
            if (CollUtil.isNotEmpty(missingTaskIds)) {
                List<TaskDO> missingTasks = taskMapper.getByIdList(missingTaskIds);
                taskDOList.addAll(missingTasks);
            }
        }

        // 构建各种映射关系
        Map<Long, String> taskNameMap = taskDOList.stream()
                .collect(Collectors.toMap(TaskDO::getId, TaskDO::getName, (oldVal, newVal) -> newVal));

        Map<Long, BigDecimal> taskUseTimeMap = taskDOList.stream().collect(Collectors.toMap(TaskDO::getId, TaskDO::getPlanUseTime, (oldVal, newVal) -> newVal));

        Map<String, List<Long>> executorTaskMap = personMapper.get(
                        taskDOList.stream().map(TaskDO::getId).collect(Collectors.toList()),
                        PersonTypeEnum.TASK_EXECUTOR.getCode())
                .stream()
                .collect(Collectors.groupingBy(
                        PersonDO::getUserId,
                        Collectors.mapping(PersonDO::getMainId, Collectors.toList())
                ));

        Map<Long, List<WorkHoursRecordDO>> taskWorkRecordMap = hoursRecordDOList.stream()
                .collect(Collectors.groupingBy(WorkHoursRecordDO::getWorkItemId));

        List<Long> projectIds = taskDOList.stream().map(TaskDO::getProjectId).collect(Collectors.toList());
        if (CollUtil.isEmpty(projectIds)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        Map<Long, String> projectMap = projectMapper.getByIds(projectIds)
                .stream()
                .collect(Collectors.toMap(ProjectDO::getId, ProjectDO::getName));

        Map<Long, Long> taskProjectMap = taskDOList.stream()
                .collect(Collectors.toMap(TaskDO::getId, TaskDO::getProjectId));

        // 构建结果列表
        List<WorkHoursOverviewVO> workHoursOverviewVOList = createManMap.entrySet().stream()
                .map(entry -> buildWorkHoursOverviewVO(entry, executorTaskMap, taskWorkRecordMap,
                        taskNameMap, projectMap, taskProjectMap, workDays, taskUseTimeMap))
                .collect(Collectors.toList());

        // 过滤掉任务列表为空的数据
        workHoursOverviewVOList = workHoursOverviewVOList.stream()
                .filter(vo -> !vo.getWorkHoursTasks().isEmpty())
                .collect(Collectors.toList());

        // 过滤未登记
        if (Boolean.TRUE.equals(query.getIsUnregistered())) {
            workHoursOverviewVOList = workHoursOverviewVOList.stream()
                    .filter(vo -> !vo.getUnregisteredDateList().isEmpty())
                    .collect(Collectors.toList());
        }

        // 分页处理
        return BaseResult.success(paginateResult(workHoursOverviewVOList, query.getPageNum(), query.getPageSize()));
    }

    private Map<String, String> buildCreateManMap(OverviewWorkHoursQueryList query) {
        Map<String, String> createManMap = new HashMap<>();
        List<Long> projectIdList = query.getProjectIds();
        List<String> memberIds = query.getMemberIds();

        if (CollUtil.isNotEmpty(query.getDeptIds())) {
            try {
                memberIds.addAll(homePageService.getCacheAccounts(query.getDeptIds()));
            } catch (IOException e) {
                throw new BaseBizRuntimeException("获取部门下的成员失败");
            }
        }

        if (CollectionUtils.isNotEmpty(projectIdList)) {
            // 按项目查询人员信息
            for (Long projectId : projectIdList) {
                List<PersonDO> personDOList = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
                Map<String, String> projectPersonMap = personDOList.stream()
                        .filter(personDO -> CollectionUtils.isEmpty(memberIds) || memberIds.contains(personDO.getUserId()))
                        .filter(personDO -> StringUtils.isNotBlank(personDO.getUserId()) && StringUtils.isNotBlank(personDO.getUserName()))
                        .collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName, (oldVal, newVal) -> oldVal));
                createManMap.putAll(projectPersonMap);
            }
        } else {
            // 查询所有项目人员信息
            List<PersonDO> personDOList = personComponent.select(null, PersonTypeEnum.PROJECT_MEMBER.getCode());
            createManMap.putAll(personDOList.stream()
                    .filter(personDO -> CollectionUtils.isEmpty(memberIds) || memberIds.contains(personDO.getUserId()))
                    .filter(personDO -> StringUtils.isNotBlank(personDO.getUserId()) && StringUtils.isNotBlank(personDO.getUserName()))
                    .collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName, (oldVal, newVal) -> oldVal)));
        }

        return createManMap;
    }

    private WorkHoursOverviewVO buildWorkHoursOverviewVO(Map.Entry<String, String> entry,
                                                         Map<String, List<Long>> executorTaskMap,
                                                         Map<Long, List<WorkHoursRecordDO>> taskWorkRecordMap,
                                                         Map<Long, String> taskNameMap,
                                                         Map<Long, String> projectMap,
                                                         Map<Long, Long> taskProjectMap,
                                                         List<String> workDays,
                                                         Map<Long, BigDecimal> taskUseTimeMap) {
        String userId = entry.getKey();
        String userName = entry.getValue();

        WorkHoursOverviewVO vo = new WorkHoursOverviewVO();
        vo.setTeamMemberId(userId);
        vo.setTeamMemberName(userName);

        List<Long> taskIdList = executorTaskMap.getOrDefault(userId, Collections.emptyList());
        List<WorkHoursTaskVO> workHoursTaskVOS = new ArrayList<>(taskIdList.size());
        List<String> registeredDateList = new ArrayList<>();
        List<String> taskDateEmptyList = new ArrayList<>();

        for (Long taskId : taskIdList) {
            WorkHoursTaskVO taskVO = new WorkHoursTaskVO();
            Long projectId = taskProjectMap.get(taskId);

            taskVO.setProjectId(projectId);
            taskVO.setProjectName(projectMap.get(projectId));
            taskVO.setWorkItemName(taskNameMap.get(taskId));
            taskVO.setWorkItemType(BizTypeEnum.TASK.getCode());
            taskVO.setWorkItemId(taskId);
            taskVO.setEstimateHours(taskUseTimeMap.getOrDefault(taskId, BigDecimal.ZERO));

            List<WorkHoursRecordDO> recordList = taskWorkRecordMap.getOrDefault(taskId, Collections.emptyList());
            taskVO.setWorkHoursRecords(
                    recordList.stream()
                            .map(WorkHoursRecordCopier.INSTANCE::convert)
                            .collect(Collectors.toList())
            );
            taskVO.setTotalHours(recordList.stream()
                    .map(WorkHoursRecordDO::getWorkHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            List<String> taskRegisteredDates = recordList.stream()
                    .map(WorkHoursRecordDO::getRegistrationDate)
                    .map(date -> DATE_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()))
                    .distinct()
                    .collect(Collectors.toList());

            registeredDateList.addAll(taskRegisteredDates);
            workHoursTaskVOS.add(taskVO);
        }

        vo.setWorkHoursTasks(workHoursTaskVOS);

        List<String> notRegisteredDateList = workDays.stream()
                .filter(date -> !registeredDateList.contains(date))
                .collect(Collectors.toList());

        // 查询当前用户进行中的任务
        List<TaskDO> progressTaskList = taskMapper.getProgressTaskList(TaskListCondition.builder()
                .status(TASK_STATUSES)
                .currentDates(notRegisteredDateList)
                .ids(taskIdList)
                .build());

        // 判断当前用户进行中的任务
        for (String date : notRegisteredDateList) {
            if (CollectionUtils.isNotEmpty(progressTaskList)
                    && progressTaskList.stream().anyMatch(task -> {
                try {
                    LocalDate targetDate = LocalDate.parse(date);
                    if (task.getPlanStartDate() == null || task.getPlanEndDate() == null) {
                        return false;
                    }

                    LocalDate startDate = task.getPlanStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate endDate = task.getPlanEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                    return (targetDate.isEqual(startDate) || targetDate.isAfter(startDate))
                            && (targetDate.isEqual(endDate) || targetDate.isBefore(endDate));
                } catch (Exception e) {
                    return false;
                }
            })) {
                taskDateEmptyList.add(date);
            }
        }

        vo.setUnregisteredDateList(taskDateEmptyList);

        return vo;
    }

    private PageQueryResult<WorkHoursOverviewVO> paginateResult(List<WorkHoursOverviewVO> workHoursOverviewVOList,
                                                                int pageNum, int pageSize) {
        int total = workHoursOverviewVOList.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<WorkHoursOverviewVO> paginatedList = workHoursOverviewVOList.subList(fromIndex, toIndex);

        PageQueryResult<WorkHoursOverviewVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(paginatedList);
        pageQueryResult.setTotalItems(total);
        pageQueryResult.setCurrentPage(pageNum);
        pageQueryResult.setItemsPerPage(pageSize);
        pageQueryResult.setTotalPages((int) Math.ceil((double) total / pageSize));

        return pageQueryResult;
    }

    private WorkbenchesWorkHoursVO getWorkbenchesWorkHoursVO(PersonDO member, BigDecimal[] hours, LocalDate today) {
        WorkbenchesWorkHoursVO workHoursVO = new WorkbenchesWorkHoursVO();
        workHoursVO.setTeamMemberId(member.getUserId());
        workHoursVO.setTeamMemberName(member.getUserName());
        workHoursVO.setTotalHours(hours[0]);
        workHoursVO.setTodayHours(hours[1]);
        workHoursVO.setYesterdayHours(hours[2]);
        workHoursVO.setFridayHours(hours[3]);
        workHoursVO.setIsMonday(today.getDayOfWeek() == DayOfWeek.MONDAY);
        return workHoursVO;
    }

    private Comparator<WorkbenchesWorkHoursVO> buildComparator(String collation) {
        // 默认升序
        boolean isAsc = collation.endsWith("asc") || !collation.contains("desc");

        // 提取排序字段
        String field = collation.contains(" ") ? collation.split(" ")[0] : collation;

        Comparator<WorkbenchesWorkHoursVO> comparator;

        switch (field) {
            case "totalHours":
                comparator = Comparator.comparing(WorkbenchesWorkHoursVO::getTotalHours);
                break;
            case "todayHours":
                comparator = Comparator.comparing(WorkbenchesWorkHoursVO::getTodayHours);
                break;
            case "yesterdayHours":
                comparator = Comparator.comparing(WorkbenchesWorkHoursVO::getYesterdayHours);
                break;
            default:
                comparator = Comparator.comparing(WorkbenchesWorkHoursVO::getTeamMemberName);
        }

        return isAsc ? comparator : comparator.reversed();
    }


    private String buildCreateMan(UserInfo userInfo) {
        return userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
    }
}

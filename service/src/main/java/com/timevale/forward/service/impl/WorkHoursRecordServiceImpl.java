package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.condition.WorkHoursRecordCondition;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.dao.WorkHoursRecordMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.dal.entity.WorkHoursRecordDO;
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.client.WorkHoursRecordService;
import com.timevale.forward.facade.api.query.WorkHoursRecordQueryList;
import com.timevale.forward.facade.api.request.TaskDoneReq;
import com.timevale.forward.facade.api.request.TaskExecuteReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordBatchAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordModifyReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordQueryReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.RegisterWorkHoursTaskVO;
import com.timevale.forward.facade.api.result.WorkHoursProgressVO;
import com.timevale.forward.facade.api.result.WorkHoursRecordVO;
import com.timevale.forward.facade.api.result.WorkHoursRemainVO;
import com.timevale.forward.facade.api.result.WorkbenchesWorkHoursVO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.copy.WorkHoursRecordCopier;
import com.timevale.forward.service.utils.ExceptionUtil;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
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
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<Integer> TASK_STATUSES = Arrays.asList(
            TaskStatusEnum.WAITING.getCode(),
            TaskStatusEnum.PROGRESS.getCode()
    );

    @Override
    public BaseResult<PageQueryResult<WorkHoursRecordVO>> list(WorkHoursRecordQueryList workHoursRecordQueryList) {
        log.info("工时记录列表,参数:{}", workHoursRecordQueryList);
        // 转换查询条件
        WorkHoursRecordCondition workHoursRecordCondition = WorkHoursRecordCopier.INSTANCE.convert(workHoursRecordQueryList);
        // 开始分页
        PageHelper.startPage(workHoursRecordCondition.getPageNum(), workHoursRecordCondition.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
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
        // 只有任务执行人可删除
        List<PersonDO> personDOList = personComponent.select(workHoursRecordDO.getWorkItemId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
        List<String> executorIds = PersonCopier.INSTANCE.transform(personDOList).stream().map(PersonVO::getUserId).collect(Collectors.toList());
        if (!executorIds.contains(workHoursRecordDO.getCreateManId())) {
            throw new BaseBizRuntimeException("非任务执行人不能删除该任务工时");
        }
        workHoursRecordMapper.deleteById(workHoursRecordId);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Long> add(WorkHoursRecordAddReq workTimeRecordAddReq) {
        log.info("工时记录新增,参数:{}", workTimeRecordAddReq);
        WorkHoursRecordDO workHoursRecordDO = WorkHoursRecordCopier.INSTANCE.convert(workTimeRecordAddReq);
        // 如果工时为0不登记
        if (workHoursRecordDO.getWorkHours().compareTo(BigDecimal.ZERO) == 0) {
            return BaseResult.success(null);
        }
        // 登记人
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        workHoursRecordDO.setCreateMan(buildCreateMan(userInfo));
        workHoursRecordDO.setCreateManId(userInfo.getId());
        // 检测
        saveBeforeCheckTask(workHoursRecordDO);
        // 入库
        workHoursRecordMapper.insert(workHoursRecordDO);
        return BaseResult.success(workHoursRecordDO.getId());
    }

    private void saveBeforeCheckTask(WorkHoursRecordDO workHoursRecordDO) {
        // 查询登记工时所属任务
        if (Objects.equals(BizTypeEnum.TASK.getCode(), workHoursRecordDO.getWorkItemType())) {
            TaskDO taskDO = taskMapper.getById(workHoursRecordDO.getWorkItemId());
            if (taskDO == null) {
                throw new BaseBizRuntimeException("任务不存在");
            }
            // 查询历史进度
            List<WorkHoursRecordDO> lastProgressList = workHoursRecordMapper.getLastProgress(Collections.singletonList(taskDO.getProjectId()), BizTypeEnum.TASK.getCode(), Collections.singletonList(taskDO.getId()));
            // 进度Map
            Map<Long,  Integer> lastProgressMap = lastProgressList.stream().collect(Collectors.toMap(WorkHoursRecordDO::getWorkItemId, WorkHoursRecordDO::getProgress, (v1, v2) -> v2));
            if (workHoursRecordDO.getProgress() <= lastProgressMap.getOrDefault(taskDO.getId(), 0)) {
                throw new BaseBizRuntimeException("进度不能小于等于历史进度");
            }
            // 获取当前日期
            LocalDate today = LocalDate.now();
            // 前一天的开始时间（00:00:00）
            LocalDateTime startTime = today.atStartOfDay();
            // 前一天的结束时间（23:59:59.999999999）
            LocalDateTime endTime = today.atTime(LocalTime.MAX);
            // 登记人当日已登记工时
            BigDecimal remainingHourDeviation = workHoursRecordMapper.list(WorkHoursRecordCondition.builder()
                    .projectId(taskDO.getProjectId())
                    .workItemType(workHoursRecordDO.getWorkItemType())
                    .workItemId(workHoursRecordDO.getWorkItemId())
                    .createManId(workHoursRecordDO.getCreateManId())
                    .stratTime(startTime.format(DATE_TIME_FORMATTER))
                    .endTime(endTime.format(DATE_TIME_FORMATTER))
                    .build())
                    .stream()
                    .map(WorkHoursRecordDO::getWorkHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 检查该任务当日是否已经登记满24小时工时
            if (remainingHourDeviation.add(workHoursRecordDO.getWorkHours()).compareTo(new BigDecimal(24)) > 0) {
                throw new BaseBizRuntimeException("该任务当日工时超出24小时，请重新填写");
            }

            // 如果任务没有开启执行，则登记工时直接开启任务
            if (TaskStatusEnum.WAITING.getCode().equals(taskDO.getStatus())) {
                TaskExecuteReq taskExecuteReq = new TaskExecuteReq();
                taskExecuteReq.setId(taskDO.getId());
                taskExecuteReq.setActualStartDate(new Date());
                taskService.execute(taskExecuteReq);
            }
            // 如果任务进度是100，则任务直接完成
            if (workHoursRecordDO.getProgress() >= 100) {
                TaskDoneReq taskDoneReq = new TaskDoneReq();
                taskDoneReq.setId(taskDO.getId());
                taskDoneReq.setActualEndDate(new Date());
                taskService.done(taskDoneReq);
            }
            // 执行人
            List<PersonDO> personDOList = personComponent.select(taskDO.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
            List<String> executorIds = PersonCopier.INSTANCE.transform(personDOList).stream().map(PersonVO::getUserId).collect(Collectors.toList());
            if (!executorIds.contains(workHoursRecordDO.getCreateManId())) {
                throw new BaseBizRuntimeException("非任务执行人不能登记该任务工时");
            }
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
        workHoursRecordDO.setCreateManId(recordDO.getCreateManId());
        workHoursRecordDO.setCreateMan(recordDO.getCreateMan());
        // 检测
        saveBeforeCheckTask(workHoursRecordDO);
        workHoursRecordMapper.update(workHoursRecordDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<WorkHoursRecordVO> get(Long workHoursRecordId) {
        log.info("工时记录查看接收参数:{}", workHoursRecordId);
        WorkHoursRecordDO workHoursRecordDO = workHoursRecordMapper.getById(workHoursRecordId);
        if (workHoursRecordDO == null) {
            throw new BaseBizRuntimeException("该工时记录不存在");
        }
        WorkHoursRecordVO workHoursRecordVO = WorkHoursRecordCopier.INSTANCE.convert(workHoursRecordDO);

        return BaseResult.success(workHoursRecordVO);
    }

    @Override
    public BaseResult<Boolean> batchAdd(WorkHoursRecordBatchAddReq workHoursRecordBatchAddReq) {
        log.info("工时记录批量新增接收参数:{}", workHoursRecordBatchAddReq);
        List<WorkHoursRecordAddReq> workHoursSimples = workHoursRecordBatchAddReq.getWorkHoursSimples();
        if (CollectionUtils.isEmpty(workHoursSimples)) {
            return BaseResult.success(true);
        }

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String createMan = buildCreateMan(userInfo);
        String createManId = userInfo.getId();

        List<Future<?>> futures = new ArrayList<>();
        for (WorkHoursRecordAddReq a : workHoursSimples) {
            Future<?> future = threadPoolTaskExecutor.submit(() -> {
                // 转换
                WorkHoursRecordDO workHoursRecordDO = WorkHoursRecordCopier.INSTANCE.convert(a);
                workHoursRecordDO.setCreateMan(createMan);
                workHoursRecordDO.setCreateManId(createManId);
                saveBeforeCheckTask(workHoursRecordDO);
                workHoursRecordMapper.insert(workHoursRecordDO);
            });
            futures.add(future);
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new BaseBizRuntimeException(ExceptionUtil.getRootExpMsg(e));
            }
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

            List<WorkHoursRecordDO> hoursRecordDOList = workHoursRecordMapper.list(WorkHoursRecordCondition.builder()
                    .projectId(taskDO.getProjectId())
                    .workItemType(workHoursRecordQueryReq.getWorkItemType())
                    .workItemId(workHoursRecordQueryReq.getWorkItemId())
                    .build());
            // 填报总工时
            BigDecimal sum = hoursRecordDOList.stream().map(WorkHoursRecordDO::getWorkHours).reduce(BigDecimal.ZERO, BigDecimal::add);

            // 最新工时进度
            Integer progress = hoursRecordDOList.stream().map(WorkHoursRecordDO::getProgress).max(Comparator.comparingInt(Integer::intValue)).orElse(0);

            // 剩余工时
            BigDecimal remainingManHour = planUseTime.subtract(sum);

            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            String createManId = userInfo.getId();

            // 当日已登记工时
            BigDecimal remainingHourDeviation = hoursRecordDOList.stream()
                    .filter(a -> createManId.equals(a.getCreateManId()) && DateUtil.isSameDay(a.getCreateDate(), new Date()))
                    .map(WorkHoursRecordDO::getWorkHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 当日剩余工时
            BigDecimal remainingHour = new BigDecimal(24).subtract(remainingHourDeviation);

            workHoursRemainVO.setEstimatedHours(planUseTime);
            workHoursRemainVO.setTotalManHour(sum);
            workHoursRemainVO.setLatestProgress(progress);
            workHoursRemainVO.setRemainingManHour(remainingManHour);
            workHoursRemainVO.setRemainingHourDeviation(remainingHour);
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
    public BaseResult<List<RegisterWorkHoursTaskVO>> waitRegisterTaskList() {
        List<RegisterWorkHoursTaskVO> registerWorkHoursTaskVOS = new ArrayList<>();
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<Long> executorTaskIds = personMapper.getMainIds(Lists.newArrayList(userInfo.getId()), null, PersonTypeEnum.TASK_EXECUTOR.getCode());
        // 是否存在进行中的任务
        if (executorTaskIds.isEmpty()) {
            return BaseResult.success(registerWorkHoursTaskVOS);
        }
        // 查询当前用户进行中的任务
        List<TaskDO> progressTaskList = taskMapper.getProgressTaskList(TaskListCondition.builder()
                .status(TASK_STATUSES)
                .currentDate(new Date())
                .ids(executorTaskIds)
                .build());
        // 是否存在进行中的任务
        if (progressTaskList.isEmpty()) {
            return BaseResult.success(registerWorkHoursTaskVOS);
        }
        // 查询任务对应的项目名称
        List<Long> projectIds = progressTaskList.stream().map(TaskDO::getProjectId).collect(Collectors.toList());
        Map<Long, String> projectMap = projectMapper.getByIds(projectIds).stream().collect(Collectors.toMap(ProjectDO::getId, ProjectDO::getName, (v1, v2) -> v1));

        // 任务列表
        for (TaskDO taskDO : progressTaskList) {
            RegisterWorkHoursTaskVO registerWorkHoursTaskVO = RegisterWorkHoursTaskVO.builder()
                    .workItemId(taskDO.getId())
                    .projectId(taskDO.getProjectId())
                    .projectName(projectMap.get(taskDO.getProjectId()))
                    .name(taskDO.getName())
                    .workItemType(BizTypeEnum.TASK.getCode())
                    .build();
            registerWorkHoursTaskVOS.add(registerWorkHoursTaskVO);
        }
        return BaseResult.success(registerWorkHoursTaskVOS);
    }

    @Override
    public BaseResult<List<WorkbenchesWorkHoursVO>> workbenches(List<Long> bizDomainIds, List<Long> productLineIds, List<Long> projectIds, List<Integer> status) {
        // 1. 批量获取项目数据
        List<Long> projectIdList = projectMapper.getProjectIds(projectIds, productLineIds, bizDomainIds);
        if (projectIdList.isEmpty()) {
            return BaseResult.success(Collections.emptyList());
        }

        // 2. 并行获取所有必要数据
        List<ProjectDO> projectDOList = projectMapper.getByIds(projectIdList);
        if (CollUtil.isNotEmpty(status)) {
            projectDOList = projectDOList.stream().filter(e -> status.contains(e.getStatus())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(projectDOList)) {
            return BaseResult.success(Collections.emptyList());
        }

        // 3. 批量查询工时记录和人员信息
        WorkHoursRecordCondition condition = WorkHoursRecordCondition.builder().projectIds(projectIdList).build();
        List<WorkHoursRecordDO> hoursRecordDOList = workHoursRecordMapper.list(condition);
        List<PersonDO> personDOList = personMapper.get(projectIdList, PersonTypeEnum.PROJECT_MEMBER.getCode());

        // 4. 预处理数据映射
        Map<Long, List<WorkHoursRecordDO>> projectWorkHoursMap = hoursRecordDOList.stream()
                .collect(Collectors.groupingBy(WorkHoursRecordDO::getProjectId));

        Map<Long, List<PersonDO>> projectMembersMap = personDOList.stream()
                .collect(Collectors.groupingBy(PersonDO::getMainId));

        // 5. 准备时间范围
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zoneId);
        LocalDate yesterday = today.minusDays(1);

        // 6. 处理工时记录的时间映射
        Map<Long, LocalDate> recordDateMap = hoursRecordDOList.stream()
                .collect(Collectors.toMap(
                        WorkHoursRecordDO::getId,
                        record -> record.getCreateDate().toInstant().atZone(zoneId).toLocalDate()
                ));

        // 7. 构建结果
        List<WorkbenchesWorkHoursVO> result = projectDOList.parallelStream()
                .map(projectDO -> {
                    Long projectId = projectDO.getId();
                    WorkbenchesWorkHoursVO vo = new WorkbenchesWorkHoursVO();
                    vo.setProject(ProjectCopier.INSTANCE.transform(projectDO));

                    List<PersonDO> members = projectMembersMap.getOrDefault(projectId, Collections.emptyList());
                    List<WorkHoursRecordDO> records = projectWorkHoursMap.getOrDefault(projectId, Collections.emptyList());

                    // 按成员分组工时记录
                    Map<String, List<WorkHoursRecordDO>> memberRecordsMap = records.stream()
                            .collect(Collectors.groupingBy(WorkHoursRecordDO::getCreateManId));

                    List<WorkbenchesWorkHoursVO.TeamMemberHours> memberHours = members.stream()
                            .map(member -> {
                                List<WorkHoursRecordDO> memberRecords = memberRecordsMap.getOrDefault(member.getUserId(), Collections.emptyList());

                                // 计算各类工时
                                BigDecimal[] hours = memberRecords.stream()
                                        .collect(
                                                // total, today, yesterday
                                                () -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO},
                                                (acc, record) -> {
                                                    LocalDate recordDate = recordDateMap.get(record.getId());
                                                    BigDecimal workHours = record.getWorkHours();

                                                    // 总工时
                                                    acc[0] = acc[0].add(workHours);
                                                    if (recordDate.equals(today)) {
                                                        // 今日工时
                                                        acc[1] = acc[1].add(workHours);
                                                    } else if (recordDate.equals(yesterday)) {
                                                        // 昨日工时
                                                        acc[2] = acc[2].add(workHours);
                                                    }
                                                },
                                                (a, b) -> {
                                                    a[0] = a[0].add(b[0]);
                                                    a[1] = a[1].add(b[1]);
                                                    a[2] = a[2].add(b[2]);
                                                }
                                        );

                                WorkbenchesWorkHoursVO.TeamMemberHours teamMemberHours = new WorkbenchesWorkHoursVO.TeamMemberHours();
                                teamMemberHours.setTeamMemberId(member.getUserId());
                                teamMemberHours.setTeamMemberName(member.getUserName());
                                teamMemberHours.setTotalHours(hours[0]);
                                teamMemberHours.setTodayHours(hours[1]);
                                teamMemberHours.setYesterdayHours(hours[2]);

                                return teamMemberHours;
                            })
                            .collect(Collectors.toList());

                    vo.setMemberHoursList(memberHours);
                    return vo;
                })
                .sorted(Comparator.comparing(e -> e.getProject().getPlanStartDate(), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return BaseResult.success(result);
    }

    private String buildCreateMan(UserInfo userInfo) {
        return userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
    }
}

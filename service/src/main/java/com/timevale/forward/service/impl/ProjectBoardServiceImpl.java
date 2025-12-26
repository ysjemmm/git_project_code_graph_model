package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.WorkHoursRecordCondition;
import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.dao.TaskProductDemandMapper;
import com.timevale.forward.dal.dao.TestBillMapper;
import com.timevale.forward.dal.dao.WorkHoursRecordMapper;
import com.timevale.forward.dal.dto.BugOfflineBelongDistributionDTO;
import com.timevale.forward.dal.dto.BugOfflineCountDTO;
import com.timevale.forward.dal.dto.BugOfflineReasonDistributionDTO;
import com.timevale.forward.dal.dto.TaskOverdueDTO;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.dal.entity.TaskProductDemandDO;
import com.timevale.forward.dal.entity.TestBillDO;
import com.timevale.forward.dal.entity.WorkHoursRecordDO;
import com.timevale.forward.facade.api.client.ProjectBoardService;
import com.timevale.forward.facade.api.query.ProjectBugOfflineCountQueryList;
import com.timevale.forward.facade.api.query.TaskOverdueRankQueryList;
import com.timevale.forward.facade.api.result.BugOfflineAllCountVO;
import com.timevale.forward.facade.api.result.BugOfflineBelongDistributionVO;
import com.timevale.forward.facade.api.result.BugOfflineCountVO;
import com.timevale.forward.facade.api.result.BugOfflineReasonDistributionVO;
import com.timevale.forward.facade.api.result.BugOfflineTrendVO;
import com.timevale.forward.facade.api.result.ProjectBoardDataIndicatorVO;
import com.timevale.forward.facade.api.result.ProjectBoardDemandWorkTimeVO;
import com.timevale.forward.facade.api.result.ProjectBoardSinglelWorkTimeVO;
import com.timevale.forward.facade.api.result.ProjectBoardTaskVO;
import com.timevale.forward.facade.api.result.TaskOverdueCountVO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.enums.BugStatusEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProjectRiskStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.model.enums.TestBillResultEnum;
import com.timevale.forward.model.enums.TestBillStatusEnum;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.copy.BugOfflineCopier;
import com.timevale.forward.service.copy.TaskCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.enums.BaseResultCodeEnum;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RestService
public class ProjectBoardServiceImpl implements ProjectBoardService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProductDemandComponent productDemandComponent;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private SqlOrderComponent sqlOrderComponent;

    @Resource
    private TaskProductDemandMapper taskProductDemandMapper;

    @Resource
    private TestBillMapper testBillMapper;

    @Resource
    private BugOfflineMapper bugOfflineMapper;

    @Resource
    private ProjectRiskMapper projectRiskMapper;

    @Resource
    private BugLogMapper bugLogMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private WorkHoursRecordMapper workHoursRecordMapper;

    @Override
    public BaseResult<ProjectBoardDataIndicatorVO> getDataIndicator(Long projectId) {

        List<Long> productDemandIdList = projectProductDemandMapper.getByProjectId(projectId)
                .stream()
                .map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        log.info("[getDataIndicator]项目关联的产品需求数: {}", productDemandIdList.size());

        List<TaskDO> taskDOList = taskMapper.getByProjectId(projectId);
        taskDOList = taskDOList.stream().filter(e -> !TaskStatusEnum.INVALID.getCode().equals(e.getStatus())).collect(Collectors.toList());
        log.info("[getDataIndicator]项目关联的任务数: {}", taskDOList.size());

        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByProjectId(projectId);
        log.info("[getDataIndicator]项目关联的线下bug: {}", bugOfflineDOList.size());

        TestBillDO testBillDO = testBillMapper.selectByProjectId(projectId);
        log.info("[getDataIndicator]项目的提测单: {}", testBillDO);

        List<TaskProductDemandDO> taskProductDemandDOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(productDemandIdList)) {
            taskProductDemandDOList = taskProductDemandMapper.selectByProductDemandId(productDemandIdList);
        }
        log.info("[getDataIndicator]任务-产品需求关联关系: {}", taskProductDemandDOList);

        List<ProjectRiskDO> projectRiskDOList = projectRiskMapper.selectByProjectId(projectId);
        log.info("[getDataIndicator]项目风险: {}", projectRiskDOList);

        // 结果
        ProjectBoardDataIndicatorVO result = new ProjectBoardDataIndicatorVO();

        // 项目任务进度
        BigDecimal completedTime = new BigDecimal("0");
        BigDecimal planUseTime = new BigDecimal("0");
        for (TaskDO e : taskDOList) {
            planUseTime = planUseTime.add(e.getPlanUseTime());
            if (TaskStatusEnum.DONE.getCode().equals(e.getStatus())) {
                completedTime = completedTime.add(e.getPlanUseTime());
            }
        }
        BigDecimal absolutely = new BigDecimal("100.00");
        if (completedTime.compareTo(planUseTime) == 0) {
            result.setTaskProgress(absolutely.toString());
        } else {
            result.setTaskProgress(completedTime.multiply(absolutely).divide(planUseTime, 2, RoundingMode.DOWN).toString());
        }

        // 总产品需求数、总任务数、总线下bug数
        result.setProductDemandCount(productDemandIdList.size());
        result.setTaskCount(taskDOList.size());
        result.setBugOfflineCount(bugOfflineDOList.size());

        // 提测结果
        if (testBillDO == null || TestBillStatusEnum.PRE_SUBMIT_TEST_CASE.getCode().equals(testBillDO.getStatus())) {
            result.setSubmitTestResult(TestBillResultEnum.NO_START.getText());
        } else if (TestBillStatusEnum.TEST_SUCCESS.getCode().equals(testBillDO.getStatus())) {
            result.setSubmitTestResult(TestBillResultEnum.SUCCESS.getText());
        } else if (testBillDO.getReturnCount() > 0 && TestBillStatusEnum.NO_SELF_TEST.getCode().equals(testBillDO.getStatus())) {
            result.setSubmitTestResult(TestBillResultEnum.FAIL.getText());
        } else {
            result.setSubmitTestResult(TestBillResultEnum.TESTING.getText());
        }

        // 今日Date
        Date today = new Date();

        // 待完成任务数、逾期任务数
        result.setWaitingTaskCount((int) taskDOList.stream().filter(e -> !TaskStatusEnum.DONE.getCode().equals(e.getStatus())).count());
        result.setOverdueTaskCount((int) taskDOList.stream().filter(e -> {
            Date date = e.getActualEndDate() == null ? today : e.getActualEndDate();
            return date.compareTo(e.getPlanEndDate()) > 0;
        }).count());

        // 今日应完成任务数、今日待完成任务数
        List<TaskDO> todayTaskList = taskDOList.stream().filter(e -> DateUtil.getIntervalDays(e.getPlanEndDate(), today) == 0).collect(Collectors.toList());
        result.setCompleteTaskToday(todayTaskList.size());
        result.setCompleteTaskTodayRemain((int) todayTaskList.stream().filter(e -> !TaskStatusEnum.DONE.getCode().equals(e.getStatus())).count());

        // 未拆解任务需求数
        int productDemandCount = productDemandIdList.size();
        int dismantleDemandCount = (int) taskProductDemandDOList.stream().map(TaskProductDemandDO::getProductDemandId).distinct().count();
        result.setNotDismantleDemand(productDemandCount - dismantleDemandCount);

        // 待处理项目风险数
        result.setWaitingRiskCount((int) projectRiskDOList.stream().filter(e -> ProjectRiskStatusEnum.PENDING.getCode().equals(e.getStatus())).count());

        // 待开发解决线下bug数、待验证线下bug数、延期修复bug数
        int waitingSolve = (int) bugOfflineDOList.stream().filter(e -> BugStatusEnum.OPEN.getCode().equals(e.getStatus())
                || BugStatusEnum.REPAIR.getCode().equals(e.getStatus())).count();
        int waitingCheck = (int) bugOfflineDOList.stream().filter(e -> BugStatusEnum.ACCEPTANCE.getCode().equals(e.getStatus())
                || BugStatusEnum.CONFIRM.getCode().equals(e.getStatus())).count();
        int postRepair = (int) bugOfflineDOList.stream().filter(e -> BugStatusEnum.POSTPONE_REPAIR.getCode().equals(e.getStatus())).count();

        result.setWaitingSolveBugOfflineCount(waitingSolve);
        result.setWaitingCheckBugOfflineCount(waitingCheck);
        result.setPostponeRepairBugOfflineCount(postRepair);


        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<BugOfflineTrendVO>> getBoardBugOfflineTrend(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            return BaseResult.fail(BaseResultCodeEnum.DATA_ERROR.getNCode(),"项目不存在");
        }

        // 线下bug
        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByProjectId(projectId);
        List<Long> bugOfflineIdList = bugOfflineDOList.stream().map(BugOfflineDO::getId).collect(Collectors.toList());

        // 线下bug日志
        List<BugLogDO> newLogList = new ArrayList<>();
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bugOfflineIdList)) {
            bugLogDOList = bugLogMapper.selectBugStatusLog(bugOfflineIdList, BugLogTypeEnum.OFFLINE.getCode());
        }

        // 日志分组 by id
        Map<Long, List<BugLogDO>> logMap = bugLogDOList.stream().collect(Collectors.groupingBy(BugLogDO::getMainId));
        logMap.forEach((k, v) -> v.stream()
                .max(Comparator.comparing(BaseDO::getCreateDate))
                .ifPresent(e -> {
                    if (BugStatusEnum.completed(e.getNewValue())) {
                        newLogList.add(e);
                    }
                }));

        // 结果
        List<BugOfflineTrendVO> result = new ArrayList<>();

        // 项目开始和结束时间
        Date endDate = projectDO.getActualEndDate() == null ? projectDO.getPlanEndDate() : projectDO.getActualEndDate();
        Date startDate = projectDO.getActualStartDate() == null ? projectDO.getPlanStartDate() : projectDO.getActualStartDate();

        // 当前时间和结束时间取小值
        endDate = DateUtil.min(endDate, new Date());

        // 当天的最大
        endDate = DateUtil.getEndOfDay(endDate);
        startDate = DateUtil.getStartOfDay(startDate);

        // 时间指针
        Date point = startDate;
        while (point.compareTo(endDate) <= 0) {
            Date pointEnd = DateUtil.getEndOfDay(point);

            // 日期、累积创建数量、累积解决数量
            BugOfflineTrendVO trendVO = new BugOfflineTrendVO();
            trendVO.setDate(point);
            trendVO.setCreatedBug((int) bugOfflineDOList.stream().filter(e -> pointEnd.compareTo(e.getCreateDate()) >= 0).count());
            trendVO.setSolvedBug((int) newLogList.stream().filter(e -> pointEnd.compareTo(e.getCreateDate()) >= 0).count());

            result.add(trendVO);

            point = DateUtil.addDay(point, 1);
        }

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<ProjectBoardSinglelWorkTimeVO>> getWorkTime(Long projectId) {
        log.info("人员工时,参数:{}", projectId);
        List<ProjectBoardSinglelWorkTimeVO> result = new ArrayList<>();
        List<TaskDO> taskDos = taskMapper.getByProjectId(projectId);
        List<TaskDO> filtered = filterValidTasks(taskDos);

        if (CollectionUtils.isEmpty(filtered)) {
            return BaseResult.success(result);
        }

        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }

        Date projectStartDate = projectDO.getActualStartDate() == null ? projectDO.getPlanStartDate() : projectDO.getActualStartDate();
        List<Date> projectEndDate = new ArrayList<>();
        if (projectDO.getActualEndDate() != null) {
            List<ProjectNodeDO> projectNodeDos = projectNodeMapper.get(projectId);
            Optional<ProjectNodeDO> max = projectNodeDos.stream().filter(a -> a.getActualDate() != null).max(Comparator.comparing(ProjectNodeDO::getActualDate));
            max.ifPresent(a -> projectEndDate.add(a.getActualDate()));
        } else {
            projectEndDate.add(projectDO.getPlanEndDate());
        }

        List<Long> taskIds = filtered.stream().map(TaskDO::getId).collect(Collectors.toList());
        Map<Long, TaskDO> taskMap = filtered.stream().collect(Collectors.toMap(TaskDO::getId, k -> k, (v1, v2) -> v2));

        List<PersonDO> personDOList = personMapper.get(taskIds, PersonTypeEnum.TASK_EXECUTOR.getCode());

        // 查询工时信息
        List<WorkHoursRecordDO> workHoursRecordDOS = workHoursRecordMapper.list(WorkHoursRecordCondition.builder().projectId(projectId).workItemType(BizTypeEnum.TASK.getCode()).workItemIds(taskIds).build());

        Map<String, List<ProjectBoardTaskVO>> projectBoardTaskVoMap = new HashMap<>();
        Date current = new Date();
        personDOList.forEach(a -> {
            TaskDO taskDO = taskMap.get(a.getMainId());
            ProjectBoardTaskVO projectBoardTaskVO = TaskCopier.INSTANCE.convert2ProjectBoard(taskDO);
            setStartAndEndDates(taskDO, projectBoardTaskVO, current);
            projectBoardTaskVO.setStatusName(TaskStatusEnum.getTextByCode(taskDO.getStatus()));
            projectBoardTaskVO.setExecutor(a.getUserName().split("-")[0]);
            projectBoardTaskVO.setExecutorId(a.getUserId());
            projectBoardTaskVO.setIsDelay(isTaskDelayed(taskDO, current));
            // 设置任务进度
            projectBoardTaskVO.setNewProgress(getTaskProgressMap(workHoursRecordDOS).getOrDefault(taskDO.getId(), 0));
            // 设置任务工时信息
            projectBoardTaskVO.setTotalWorkHours(getWorkHoursMap(workHoursRecordDOS).getOrDefault(taskDO.getId(), BigDecimal.ZERO));
            projectBoardTaskVoMap.computeIfAbsent(a.getUserId(), v -> new ArrayList<>()).add(projectBoardTaskVO);
        });

        log.info("人员工时,任务:{}", projectBoardTaskVoMap);

        projectBoardTaskVoMap.forEach((k, v) -> {
            ProjectBoardSinglelWorkTimeVO singleWorkTimeVO = new ProjectBoardSinglelWorkTimeVO();

            BigDecimal planUseTime = v.stream().map(ProjectBoardTaskVO::getPlanUseTime).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            singleWorkTimeVO.setExecutor(v.get(0).getExecutor().split("-")[0]);
            singleWorkTimeVO.setExecutorId(v.get(0).getExecutorId());
            singleWorkTimeVO.setIsPm(Objects.equals(v.get(0).getExecutorId(), projectDO.getPmId()));
            singleWorkTimeVO.setTaskCount(v.size());
            singleWorkTimeVO.setProjectStartDate(projectStartDate);
            singleWorkTimeVO.setProjectEndDate(projectEndDate.get(0));
            singleWorkTimeVO.setTotalPlanUseTime(planUseTime);
            List<ProjectBoardTaskVO> sort = v.stream().sorted(Comparator.comparing(ProjectBoardTaskVO::getStartDate)).collect(Collectors.toList());
            singleWorkTimeVO.setProjectBoardTaskVos(sort);
            result.add(singleWorkTimeVO);
        });

        return BaseResult.success(result);
    }

    /**
     * 获取项目下所有任务工时统计
     */
    private Map<Long, BigDecimal> getWorkHoursMap(List<WorkHoursRecordDO> workHoursRecordDOS) {
        return workHoursRecordDOS.stream()
                .collect(Collectors.groupingBy(WorkHoursRecordDO::getWorkItemId,
                        Collectors.mapping(WorkHoursRecordDO::getWorkHours, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    /**
     * 获取任务进度
     */
    private Map<Long, Integer> getTaskProgressMap(List<WorkHoursRecordDO> workHoursRecordDOS) {
        return workHoursRecordDOS.stream()
                .collect(Collectors.groupingBy(WorkHoursRecordDO::getWorkItemId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(WorkHoursRecordDO::getRegistrationDate)),
                                workHoursRecordDO -> workHoursRecordDO.map(WorkHoursRecordDO::getProgress).orElse(0)
                        )));
    }

    @Override
    public BaseResult<PageQueryResult<TaskOverdueCountVO>> getTaskOverdueRank(TaskOverdueRankQueryList query) {
        if (StringUtils.isBlank(query.getOrderFiled())) {
            query.setOrderCollation(1);
            query.setOrderFiled("accumulateOverdueMillis");
        }
        String collation = sqlOrderComponent.buildWithoutId(query.getOrderFiled(), query.getOrderCollation());
        PageHelper.startPage(query.getPageNum(), query.getPageSize(), collation);
        List<TaskOverdueDTO> overdueList = taskMapper.getOverdueRank(query.getProjectId());
        List<TaskOverdueCountVO> res = TaskCopier.INSTANCE.convertOverdue(overdueList);
        PageInfo<TaskOverdueDTO> pageInfo = new PageInfo<>(overdueList);
        PageQueryResult<TaskOverdueCountVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(res);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<PageQueryResult<BugOfflineCountVO>> getBugOfflineCount(ProjectBugOfflineCountQueryList query) {
        if (StringUtils.isBlank(query.getOrderFiled())) {
            query.setOrderCollation(1);
            query.setOrderFiled("urgentRepairCount");
        }
        String collation = sqlOrderComponent.buildWithoutId(query.getOrderFiled(), query.getOrderCollation());
        PageHelper.startPage(query.getPageNum(), query.getPageSize(), collation);
        List<BugOfflineCountDTO> countList = bugOfflineMapper.getBugCount(query.getProjectId());
        List<BugOfflineCountVO> res = BugOfflineCopier.INSTANCE.convertCount(countList);
        PageInfo<BugOfflineCountDTO> pageInfo = new PageInfo<>(countList);
        PageQueryResult<BugOfflineCountVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(res);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<BugOfflineAllCountVO> getBugOfflineAllCount(Long projectId) {
        //统计状态为打开和待修复的bug
        List<BugOfflineCountDTO> countList = bugOfflineMapper.getBugCount(projectId);
        long waitRepairCount = countList.stream().mapToLong(BugOfflineCountDTO::getWaitRepairCount).sum();
        long urgentRepairCount = countList.stream().mapToLong(BugOfflineCountDTO::getUrgentRepairCount).sum();

        BugOfflineAllCountVO result = new BugOfflineAllCountVO();
        result.setWaitRepairCount(waitRepairCount);
        result.setUrgentRepairCount(urgentRepairCount);
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<BugOfflineReasonDistributionVO>> getProjectBugReasonDistribution(Long projectId) {
        List<BugOfflineReasonDistributionDTO> reasonDistributions = bugOfflineMapper.getReasonDistribution(projectId);
        return BaseResult.success(BugOfflineCopier.INSTANCE.convertReasonDistributions(reasonDistributions));
    }

    @Override
    public BaseResult<List<BugOfflineBelongDistributionVO>> getProjectBugBelongDistribution(Long projectId) {
        List<BugOfflineBelongDistributionDTO> belongDistributions = bugOfflineMapper.getBelongDistribution(projectId);
        return BaseResult.success(BugOfflineCopier.INSTANCE.convertBelongDistributions(belongDistributions));
    }

    /**
     * 获取项目需求工时信息
     * 根据项目ID查询并计算项目中各个需求的工时信息，包括需求的基本信息和关联的任务信息
     * @param projectId 项目ID，用于查询项目需求工时信息
     * @return 包含项目需求工时信息的列表
     */
    @Override
    public BaseResult<List<ProjectBoardDemandWorkTimeVO>> getDemandTime(Long projectId) {
        // 记录方法入口参数
        log.info("需求工时,参数:{}", projectId);
        // 初始化结果列表
        List<ProjectBoardDemandWorkTimeVO> result = new ArrayList<>();

        // 查询项目基本信息
        ProjectDO projectDO = projectMapper.get(projectId);
        // 如果项目不存在，抛出异常
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }

        // 确定项目实际开始日期，如果实际开始日期为空，则使用计划开始日期
        Date projectStartDate = projectDO.getActualStartDate() == null ? projectDO.getPlanStartDate() : projectDO.getActualStartDate();
        // 确定项目实际结束日期，如果实际结束日期为空，则使用计划结束日期
        Date projectEndDate;
        if (projectDO.getActualEndDate() != null) {
            // 获取项目节点信息，并计算实际结束日期
            List<ProjectNodeDO> projectNodeDos = projectNodeMapper.get(projectId);
            Optional<ProjectNodeDO> max = projectNodeDos.stream()
                    .filter(a -> a.getActualDate() != null)
                    .max(Comparator.comparing(ProjectNodeDO::getActualDate));
            projectEndDate = max.map(ProjectNodeDO::getActualDate).orElse(projectDO.getPlanEndDate());
        } else {
            projectEndDate = projectDO.getPlanEndDate();
        }

        // 查询项目关联的需求信息
        List<ProjectProductDemandDO> byProjectId = projectProductDemandMapper.getByProjectId(projectId);
        // 如果没有关联的需求，直接返回结果
        if (CollectionUtils.isEmpty(byProjectId)) {
            return BaseResult.success(result);
        }
        // 提取需求ID列表
        List<Long> demandIds = byProjectId.stream()
                .map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());

        // 根据需求ID列表查询关联的任务信息
        List<TaskProductDemandDO> taskProductDemandDOS = taskProductDemandMapper.selectByProductDemandId(demandIds);
        // 提取任务ID列表
        List<Long> taskIds = taskProductDemandDOS.stream()
                .map(TaskProductDemandDO::getTaskId)
                .collect(Collectors.toList());
        // 如果没有关联的任务，直接返回结果
        if (CollectionUtils.isEmpty(taskIds)) {
            return BaseResult.success(result);
        }
        // 查询任务基本信息
        List<TaskDO> taskDOS = taskMapper.getByIdList(taskIds);
        // 过滤无效任务和缺少日期信息的任务
        List<TaskDO> filtered = filterValidTasks(taskDOS);

        // 如果没有有效任务，直接返回空结果
        if (CollectionUtils.isEmpty(filtered)) {
            return BaseResult.success(result);
        }

        // 将过滤后的任务信息转换为Map，便于后续查询
        Map<Long, TaskDO> taskMap = filtered.stream()
                .collect(Collectors.toMap(TaskDO::getId, k -> k, (v1, v2) -> v2));

        // 查询需求详细信息
        List<ProductDemandListDO> productDemandListDOList = productDemandComponent.list(
                ProductDemandListCondition.builder().inProductDemandIds(demandIds).build());
        // 将需求信息转换为Map，便于后续查询
        Map<Long, ProductDemandListDO> demandListDOMap = productDemandListDOList.stream()
                .collect(Collectors.toMap(ProductDemandListDO::getId, k -> k));

        // 查询任务执行人信息
        Map<Long, PersonDO> personMap = personMapper.get(taskIds, PersonTypeEnum.TASK_EXECUTOR.getCode()).stream()
                .collect(Collectors.toMap(PersonDO::getMainId, p -> p, (v1, v2) -> v2));

        // 查询工时信息
        List<WorkHoursRecordDO> workHoursRecordDOS = workHoursRecordMapper.list(WorkHoursRecordCondition.builder().projectId(projectId).workItemType(BizTypeEnum.TASK.getCode()).workItemIds(taskIds).build());
        // 初始化任务信息Map
        Map<Long, List<ProjectBoardTaskVO>> projectBoardTaskVoMap = new HashMap<>();
        // 获取当前日期
        Date current = new Date();

        // 遍历任务信息，转换并计算任务的工时信息
        taskProductDemandDOS.forEach(taskProductDemandDO -> {
            Long taskId = taskProductDemandDO.getTaskId();
            TaskDO taskDO = taskMap.get(taskId);
            if (taskDO == null) return;

            ProjectBoardTaskVO projectBoardTaskVO = TaskCopier.INSTANCE.convert2ProjectBoard(taskDO);
            // 设置任务的开始和结束日期
            setStartAndEndDates(taskDO, projectBoardTaskVO, current);

            // 设置任务状态名称
            projectBoardTaskVO.setStatusName(TaskStatusEnum.getTextByCode(taskDO.getStatus()));
            // 设置任务执行人信息
            PersonDO person = personMap.get(taskId);
            projectBoardTaskVO.setExecutor(person != null ? person.getUserName().split("-")[0] : "未知");
            projectBoardTaskVO.setExecutorId(person != null ? person.getUserId() : "未知");

            // 判断任务是否延期
            projectBoardTaskVO.setIsDelay(isTaskDelayed(taskDO, current));

            // 设置任务进度
            projectBoardTaskVO.setNewProgress(getTaskProgressMap(workHoursRecordDOS).getOrDefault(taskId, 0));
            // 设置任务工时信息
            projectBoardTaskVO.setTotalWorkHours(getWorkHoursMap(workHoursRecordDOS).getOrDefault(taskId, BigDecimal.ZERO));

            // 将任务信息添加到对应需求的列表中
            projectBoardTaskVoMap.computeIfAbsent(taskProductDemandDO.getProductDemandId(), k -> new ArrayList<>())
                    .add(projectBoardTaskVO);
        });

        // 遍历需求信息，构建并添加到结果列表中
        projectBoardTaskVoMap.forEach((k, v) -> {
            ProductDemandListDO productDemandListDO = demandListDOMap.get(k);
            if (productDemandListDO == null) return;

            ProjectBoardDemandWorkTimeVO demandWorkTimeVO = new ProjectBoardDemandWorkTimeVO();

            BigDecimal planUseTime = v.stream().map(ProjectBoardTaskVO::getPlanUseTime).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            // 需求进度，总的任务进度除任务数量,保留两位小数
            double avgProgress = v.stream().mapToDouble(ProjectBoardTaskVO::getNewProgress).average().orElse(0.0);
            demandWorkTimeVO.setNewProgress(BigDecimal.valueOf(avgProgress).setScale(2, RoundingMode.HALF_UP).doubleValue());

            // 设置需求基本信息
            demandWorkTimeVO.setDemandId(productDemandListDO.getId());
            demandWorkTimeVO.setDemand(productDemandListDO.getName());
            demandWorkTimeVO.setOwnerId(productDemandListDO.getOwnerId());
            demandWorkTimeVO.setOwner(productDemandListDO.getOwner().split("-")[0]);
            demandWorkTimeVO.setPriority(productDemandListDO.getPriority());
            demandWorkTimeVO.setPriorityName(PriorityEnum.getTextByCode(productDemandListDO.getPriority()));
            demandWorkTimeVO.setExpectScheduleTime(productDemandListDO.getExpectScheduleTime());
            demandWorkTimeVO.setProductLineName(productDemandListDO.getProductLineName());
            demandWorkTimeVO.setBizDomainName(productDemandListDO.getBizDomainName());
            demandWorkTimeVO.setStatusName(ProductDemandStatusEnum.getTextByCode(productDemandListDO.getStatus()));
            // 设置项目开始和结束日期
            demandWorkTimeVO.setProjectStartDate(projectStartDate);
            demandWorkTimeVO.setProjectEndDate(projectEndDate);
            demandWorkTimeVO.setTotalPlanUseTime(planUseTime);
            // 设置任务信息
            demandWorkTimeVO.setTaskCount(v.size());
            List<ProjectBoardTaskVO> sortedTasks = v.stream()
                    .sorted(Comparator.comparing(ProjectBoardTaskVO::getStartDate))
                    .collect(Collectors.toList());
            demandWorkTimeVO.setProjectBoardTaskVos(sortedTasks);
            // 将需求工时信息添加到结果列表中
            result.add(demandWorkTimeVO);
        });

        // 返回结果
        return BaseResult.success(result);
    }

    private void setStartAndEndDates(TaskDO taskDO, ProjectBoardTaskVO vo, Date current) {
        if (taskDO.getActualStartDate() == null) {
            vo.setStartDate(taskDO.getPlanStartDate());
            vo.setEndDate(taskDO.getPlanEndDate());
        } else if (taskDO.getActualEndDate() != null) {
            vo.setStartDate(taskDO.getActualStartDate());
            vo.setEndDate(taskDO.getActualEndDate());
        } else if (taskDO.getActualStartDate().after(taskDO.getPlanEndDate())) {
            vo.setStartDate(taskDO.getActualStartDate());
            vo.setEndDate(current);
        } else {
            vo.setStartDate(taskDO.getActualStartDate());
            vo.setEndDate(taskDO.getPlanEndDate());
        }
    }

    private boolean isTaskDelayed(TaskDO taskDO, Date current) {
        if (taskDO.getActualEndDate() == null) {
            return current.after(taskDO.getPlanEndDate());
        }
        return taskDO.getActualEndDate().after(taskDO.getPlanEndDate());
    }

    /**
     * 过滤掉无效任务及缺少计划起止时间的任务
     */
    private List<TaskDO> filterValidTasks(List<TaskDO> taskList) {
        return taskList.stream()
                .filter(a -> !TaskStatusEnum.INVALID.getCode().equals(a.getStatus())
                        && a.getPlanStartDate() != null && a.getPlanEndDate() != null)
                .collect(Collectors.toList());
    }
}

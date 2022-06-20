package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectBoardService;
import com.timevale.forward.facade.api.result.BugOfflineTrendVO;
import com.timevale.forward.facade.api.result.ProjectBoardDataIndicatorVO;
import com.timevale.forward.facade.api.result.ProjectBoardSinglelWorkTimeVO;
import com.timevale.forward.facade.api.result.ProjectBoardTaskVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
    private TaskMapper taskMapper;

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
        if(CollectionUtils.isNotEmpty(productDemandIdList)){
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
            if(TaskStatusEnum.DONE.getCode().equals(e.getStatus())){
                completedTime = completedTime.add(e.getPlanUseTime());
            }
        }
        BigDecimal absolutely = new BigDecimal("100.00");
        if(completedTime.compareTo(planUseTime) == 0){
            result.setTaskProgress(absolutely.toString());
        }else{
            result.setTaskProgress(completedTime.multiply(absolutely).divide(planUseTime,2, RoundingMode.DOWN).toString());
        }

        // 总产品需求数、总任务数、总线下bug数
        result.setProductDemandCount(productDemandIdList.size());
        result.setTaskCount(taskDOList.size());
        result.setBugOfflineCount(bugOfflineDOList.size());

        // 提测结果
        if(testBillDO == null){
            result.setSubmitTestResult(TestBillResultEnum.NO_START.getText());
        }else if(TestBillStatusEnum.TEST_SUCCESS.getCode().equals(testBillDO.getStatus())){
            result.setSubmitTestResult(TestBillResultEnum.SUCCESS.getText());
        }else if(testBillDO.getReturnCount() > 0 && TestBillStatusEnum.NO_SELF_TEST.getCode().equals(testBillDO.getStatus())){
            result.setSubmitTestResult(TestBillResultEnum.FAIL.getText());
        }else{
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
        result.setCompleteTaskTodayRemain((int)todayTaskList.stream().filter(e -> !TaskStatusEnum.DONE.getCode().equals(e.getStatus())).count());

        // 未拆解任务需求数
        int productDemandCount = productDemandIdList.size();
        int dismantleDemandCount = (int)taskProductDemandDOList.stream().map(TaskProductDemandDO::getProductDemandId).distinct().count();
        result.setNotDismantleDemand(productDemandCount - dismantleDemandCount);

        // 待处理项目风险数
        result.setWaitingRiskCount((int) projectRiskDOList.stream().filter(e -> ProjectRiskStatusEnum.PENDING.getCode().equals(e.getStatus())).count());

        // 待开发解决线下bug数、待验证线下bug数、延期修复bug数
        int waitingSolve = (int) bugOfflineDOList.stream().filter(e -> BugStatusEnum.OPEN.getCode().equals(e.getStatus())
                || BugStatusEnum.REPAIR.getCode().equals(e.getStatus())).count();
        int waitingCheck = (int) bugOfflineDOList.stream().filter(e -> BugStatusEnum.ACCEPTANCE.getCode().equals(e.getStatus())
                || BugStatusEnum.CONFIRM.getCode().equals(e.getStatus())).count();
        int postRepair = (int)bugOfflineDOList.stream().filter(e -> BugStatusEnum.POSTPONE_REPAIR.getCode().equals(e.getStatus())).count();

        result.setWaitingSolveBugOfflineCount(waitingSolve);
        result.setWaitingCheckBugOfflineCount(waitingCheck);
        result.setPostponeRepairBugOfflineCount(postRepair);


        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<BugOfflineTrendVO>> getBoardBugOfflineTrend(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);

        // 线下bug
        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByProjectId(projectId);
        List<Long> bugOfflineIdList = bugOfflineDOList.stream().map(BugOfflineDO::getId).collect(Collectors.toList());

        // 线下bug日志
        List<BugLogDO> newLogList = new ArrayList<>();
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(bugOfflineIdList)){
            bugLogDOList = bugLogMapper.selectBugStatusLog(bugOfflineIdList, BugLogTypeEnum.OFFLINE.getCode());
        }

        // 日志分组 by id
        Map<Long, List<BugLogDO>> logMap = bugLogDOList.stream().collect(Collectors.groupingBy(BugLogDO::getMainId));
        logMap.forEach((k, v) -> v.stream()
                .max(Comparator.comparing(BaseDO::getCreateDate))
                .ifPresent(e -> {
                    if(BugStatusEnum.COMPLETE.getText().equals(e.getNewValue()) || BugStatusEnum.CLOSE.getText().equals(e.getNewValue())) {
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
        while(point.compareTo(endDate) <= 0){
            Date pointEnd = DateUtil.getEndOfDay(point);

            // 日期、累积创建数量、累积解决数量
            BugOfflineTrendVO trendVO = new BugOfflineTrendVO();
            trendVO.setDate(point);
            trendVO.setCreatedBug((int)bugOfflineDOList.stream().filter(e -> pointEnd.compareTo(e.getCreateDate()) >= 0).count());
            trendVO.setSolvedBug((int)newLogList.stream().filter(e -> pointEnd.compareTo(e.getCreateDate()) >= 0).count());

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
        List<TaskDO> filtered = taskDos.stream().filter(a -> !TaskStatusEnum.INVALID.getCode().equals(a.getStatus())
                && a.getPlanStartDate() != null && a.getPlanEndDate() != null).collect(Collectors.toList());

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

        List<PersonDO> personDos = personMapper.get(taskIds, PersonTypeEnum.TASK_EXECUTOR.getCode());

        Map<String, List<ProjectBoardTaskVO>> projectBoardTaskVoMap = new HashMap<>();
        personDos.forEach(a -> {
            ProjectBoardTaskVO projectBoardTaskVO = new ProjectBoardTaskVO();
            TaskDO taskDO = taskMap.get(a.getMainId());
            projectBoardTaskVO.setPlanStartDate(taskDO.getPlanStartDate());
            projectBoardTaskVO.setPlanEndDate(taskDO.getPlanEndDate());
            projectBoardTaskVO.setActualStartDate(taskDO.getActualStartDate());
            projectBoardTaskVO.setActualEndDate(taskDO.getActualEndDate());
            projectBoardTaskVO.setId(taskDO.getId());
            projectBoardTaskVO.setName(taskDO.getName());
            projectBoardTaskVO.setStatus(taskDO.getStatus());
            projectBoardTaskVO.setStatusName(TaskStatusEnum.getTextByCode(taskDO.getStatus()));
            projectBoardTaskVO.setPlanUseTime(taskDO.getPlanUseTime());
            projectBoardTaskVO.setExecutor(a.getUserName());
            projectBoardTaskVO.setExecutorId(a.getUserId());
            boolean delay = (taskDO.getActualEndDate() == null && new Date().after(taskDO.getPlanEndDate())) ||
                    (taskDO.getActualEndDate() != null && taskDO.getActualEndDate().after(taskDO.getPlanEndDate()));
            projectBoardTaskVO.setIsDelay(delay);
            projectBoardTaskVoMap.computeIfAbsent(a.getUserId(), v -> new ArrayList<>()).add(projectBoardTaskVO);
        });

        log.info("人员工时,任务:{}", projectBoardTaskVoMap);

        projectBoardTaskVoMap.forEach((k, v) -> {
            ProjectBoardSinglelWorkTimeVO singlelWorkTimeVO = new ProjectBoardSinglelWorkTimeVO();

            Optional<ProjectBoardTaskVO> min = v.stream().min(Comparator.comparing(ProjectBoardTaskVO::getPlanStartDate));
            min.ifPresent(projectBoardTaskVO -> singlelWorkTimeVO.setMinPlanStartDate(projectBoardTaskVO.getPlanStartDate()));

            Optional<ProjectBoardTaskVO> max = v.stream().max(Comparator.comparing(ProjectBoardTaskVO::getPlanEndDate));
            max.ifPresent(projectBoardTaskVO -> singlelWorkTimeVO.setMaxPlanEndDate(projectBoardTaskVO.getPlanEndDate()));

            BigDecimal bigDecimal = v.stream().map(ProjectBoardTaskVO::getPlanUseTime).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            singlelWorkTimeVO.setExecutor(v.get(0).getExecutor());
            singlelWorkTimeVO.setExecutorId(v.get(0).getExecutorId());
            singlelWorkTimeVO.setIsPm(Objects.equals(v.get(0).getExecutorId(), projectDO.getPmId()));
            singlelWorkTimeVO.setTaskCount(v.size());
            singlelWorkTimeVO.setProjectStartDate(projectStartDate);
            singlelWorkTimeVO.setProjectEndDate(projectEndDate.get(0));
            singlelWorkTimeVO.setTotalPlanUseTime(bigDecimal);
            List<ProjectBoardTaskVO> sort = v.stream().sorted(Comparator.comparing(ProjectBoardTaskVO::getPlanStartDate)).collect(Collectors.toList());
            singlelWorkTimeVO.setProjectBoardTaskVos(sort);
            result.add(singlelWorkTimeVO);
        });

        return BaseResult.success(result);
    }

}

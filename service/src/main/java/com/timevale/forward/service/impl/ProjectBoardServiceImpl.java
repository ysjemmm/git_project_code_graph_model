package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectBoardService;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.facade.api.result.BugOfflineTrendVO;
import com.timevale.forward.facade.api.result.ProjectBoardDataIndicatorVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateUtil;
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
    ProjectMapper projectMapper;

    @Resource
    ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    TaskMapper taskMapper;

    @Resource
    TaskProductDemandMapper taskProductDemandMapper;

    @Resource
    TestBillMapper testBillMapper;

    @Resource
    BugOfflineMapper bugOfflineMapper;

    @Resource
    ProjectRiskMapper projectRiskMapper;

    @Resource
    BugLogMapper bugLogMapper;

    @Resource
    PersonMapper personMapper;

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
        if(completedTime.compareTo(planUseTime) == 0){
            result.setTaskProgress(new BigDecimal("100.00").toString());
        }else{
            result.setTaskProgress(completedTime.divide(planUseTime, 2, RoundingMode.DOWN).toString());
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
        }else if(testBillDO.getReturnCount() > 0){
            result.setSubmitTestResult(TestBillResultEnum.FAIL.getText());
        }else{
            result.setSubmitTestResult(TestBillResultEnum.TESTING.getText());
        }

        // 今日Date
        Date today = new Date();

        // 逾期任务数、待完成任务数
        List<TaskDO> undoneTaskList = taskDOList.stream().filter(e -> !TaskStatusEnum.DONE.getCode().equals(e.getStatus())).collect(Collectors.toList());
        result.setOverdueTaskCount((int)undoneTaskList.stream().filter(e -> today.after(e.getPlanEndDate())).count());
        result.setWaitingTaskCount(undoneTaskList.size());

        // 今日应完成任务数、今日待完成任务数
        List<TaskDO> todayTaskList = taskDOList.stream().filter(e -> DateUtil.getIntervalDays(e.getPlanEndDate(), today) == 0).collect(Collectors.toList());
        result.setCompleteTaskToday(todayTaskList.size());
        result.setCompleteTaskTodayRemain((int)todayTaskList.stream().filter(e -> !TaskStatusEnum.DONE.getCode().equals(e.getStatus())).count());

        // 未拆解任务需求数
        int dismantleDemandCount = (int)taskProductDemandDOList.stream().map(TaskProductDemandDO::getProductDemandId).distinct().count();
        int productDemandCount = productDemandIdList.size();
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
        logMap.forEach((k, v) -> {
            // 日志分组 by 日期
            Map<Date, List<BugLogDO>> logMapDate = v.stream().collect(Collectors.groupingBy(e -> DateUtil.getStartOfDay(e.getCreateDate())));

            logMapDate.forEach((sk, sv) -> {
                sv.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));
                sv.stream().findFirst().ifPresent(newLogList::add);
            });
        });

        // 结果
        List<BugOfflineTrendVO> result = new ArrayList<>();

        // 项目开始和结束时间
        Date endDate = projectDO.getActualEndDate() == null ? projectDO.getPlanEndDate() : projectDO.getActualEndDate();
        Date startDate = projectDO.getActualStartDate() == null ? projectDO.getPlanStartDate() : projectDO.getActualStartDate();

        // 当前时间和结束时间取小值
        endDate = DateUtil.min(endDate, new Date());

        // 当天的最大和最小
        endDate = DateUtil.getEndOfDay(endDate);
        startDate = DateUtil.getStartOfDay(startDate);

        // 线下bug完成和关闭的数量
        HashSet<Long> completedBugSet = new HashSet<>();

        // 时间指针
        Date pointStart = startDate;
        Date pointEnd = DateUtil.getEndOfDay(startDate);

        while(pointStart.before(endDate)){
            Date finalPointEnd = pointEnd;
            Date finalPointStart = pointStart;

            BugOfflineTrendVO trendVO = new BugOfflineTrendVO();

            // 日期
            trendVO.setDate(finalPointStart);

            // 累积创建数量
            trendVO.setCreatedBug((int) bugOfflineDOList.stream().filter(e -> finalPointEnd.after(e.getCreateDate())).count());

            // 累积解决数量
            List<BugLogDO> todayBugLogDOList = newLogList.stream()
                    .filter(e -> DateUtil.inInterval(e.getCreateDate(), finalPointStart, finalPointEnd))
                    .collect(Collectors.toList());

            todayBugLogDOList.forEach(e -> {
                if(BugStatusEnum.COMPLETE.getText().equals(e.getNewValue()) || BugStatusEnum.CLOSE.getText().equals(e.getNewValue())) {
                    completedBugSet.add(e.getMainId());
                }else{
                    completedBugSet.remove(e.getMainId());
                }
            });
            trendVO.setSolvedBug(completedBugSet.size());

            result.add(trendVO);

            pointEnd = DateUtil.addDay(pointEnd, 1);
            pointStart = DateUtil.addDay(pointStart, 1);
        }

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<ProjectBoardSinglelWorkTimeVO>> getWorkTime(Long projectId) {
        log.info("人员工时,参数:{}", projectId);
        List<ProjectBoardSinglelWorkTimeVO> result = new ArrayList<>();
        List<TaskDO> taskDos = taskMapper.getByProjectId(projectId);
        List<TaskDO> filtered = taskDos.stream().filter(a -> !TaskStatusEnum.INVALID.getCode().equals(a.getStatus())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filtered)) {
            return BaseResult.success(result);
        }

        ProjectDO projectDO = projectMapper.get(projectId);
        Date projectStartDate = projectDO.getActualStartDate() == null ? projectDO.getPlanStartDate() : projectDO.getActualStartDate();
        Date projectEndDate = projectDO.getActualEndDate() == null ? projectDO.getPlanEndDate() : projectDO.getActualEndDate();

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
            singlelWorkTimeVO.setProjectEndDate(projectEndDate);
            singlelWorkTimeVO.setTotalPlanUseTime(bigDecimal);
            singlelWorkTimeVO.setProjectBoardTaskVos(v);
            result.add(singlelWorkTimeVO);
        });

        return BaseResult.success(result);
    }

}

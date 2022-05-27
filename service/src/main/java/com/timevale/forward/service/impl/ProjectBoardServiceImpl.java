package com.timevale.forward.service.impl;

import cn.hutool.core.io.unit.DataUnit;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectBoardService;
import com.timevale.forward.facade.api.request.ProjectBoardReq;
import com.timevale.forward.facade.api.result.ProjectBoardBugOfflineTrendVO;
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
        BigDecimal completedTime = new BigDecimal(0);
        BigDecimal planUseTime = new BigDecimal(0);
        for (TaskDO e : taskDOList) {
            planUseTime = planUseTime.add(e.getPlanUseTime());
            if(TaskStatusEnum.DONE.getCode().equals(e.getStatus())){
                completedTime = completedTime.add(e.getPlanUseTime());
            }
        }
        if(completedTime.equals(planUseTime)){
            result.setTaskProgress(new BigDecimal(100));
        }else{
            result.setTaskProgress(completedTime.divide(planUseTime, 2, RoundingMode.DOWN));
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
    public BaseResult<ProjectBoardBugOfflineTrendVO> getBoardBugOfflineTrend(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);

        // 线下bug
        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByProjectId(projectId);
        List<Long> bugOfflineIdList = bugOfflineDOList.stream().map(BugOfflineDO::getId).collect(Collectors.toList());

        // 结果
        List<ProjectBoardBugOfflineTrendVO> result = new ArrayList<>();


        // 线下bug日志
        List<BugLogDO> newLogList = new ArrayList<>();
        List<BugLogDO> bugLogDOList = bugLogMapper.selectBugStatusLog(bugOfflineIdList, BugLogTypeEnum.OFFLINE.getCode());

        // 日志分组 by id
        Map<Long, List<BugLogDO>> logMap = bugLogDOList.stream().collect(Collectors.groupingBy(BugLogDO::getMainId));
        logMap.forEach((k, v) -> {
            Map<Date, List<BugLogDO>> logMapDate = v.stream().collect(Collectors.groupingBy(e -> DateUtil.getStartOfDay(e.getCreateDate())));

            logMapDate.forEach((sk, sv) -> {
                sv.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));
                sv.stream().findFirst().ifPresent(newLogList::add);
            });
        });

        // 项目开始和结束时间
        Date startDate = projectDO.getActualStartDate() == null ? projectDO.getPlanStartDate() : projectDO.getActualStartDate();
        Date endDate = projectDO.getActualEndDate() == null ? projectDO.getPlanEndDate() : projectDO.getActualEndDate();

        startDate = DateUtil.getEndOfDay(startDate);
        endDate = DateUtil.getEndOfDay(endDate);

        Date pointDate = startDate;
        while(pointDate.before(endDate)){



            pointDate = DateUtil.addDay(pointDate, 1);
        }


        return null;
    }
}

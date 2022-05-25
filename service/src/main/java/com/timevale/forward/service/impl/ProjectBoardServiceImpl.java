package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.dal.entity.TestBillDO;
import com.timevale.forward.facade.api.client.ProjectBoardService;
import com.timevale.forward.facade.api.request.ProjectBoardReq;
import com.timevale.forward.facade.api.result.ProjectBoardDataIndicatorVO;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.model.enums.TestBillResultEnum;
import com.timevale.forward.model.enums.TestBillStatusEnum;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

    @Override
    public BaseResult<ProjectBoardDataIndicatorVO> getDataIndicator(ProjectBoardReq projectBoardReq) {
        Long projectId = projectBoardReq.getProjectId();

        // 项目关联的产品需求
        List<Long> productDemandIdList = projectProductDemandMapper.getByProjectId(projectId)
                .stream()
                .map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        log.info("[getDataIndicator]项目关联的产品需求数: {}", productDemandIdList.size());

        // 项目关联的任务
        List<TaskDO> taskDOList = taskMapper.getByProjectId(projectId);
        taskDOList = taskDOList.stream().filter(e -> !TaskStatusEnum.INVALID.getCode().equals(e.getStatus())).collect(Collectors.toList());
        log.info("[getDataIndicator]项目关联的任务数: {}", taskDOList.size());

        // 项目关联的线下bug
        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByProjectId(projectId);
        log.info("[getDataIndicator]项目关联的线下bug: {}", bugOfflineDOList.size());

        // 项目的提测单
        TestBillDO testBillDO = testBillMapper.selectByProjectId(projectId);
        log.info("[getDataIndicator]项目的提测单: {}", testBillDO);

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
            result.setTaskProgress(completedTime.divide(planUseTime, RoundingMode.DOWN));
        }

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

        result.setProductDemandCount(productDemandIdList.size());
        result.setTaskCount(taskDOList.size());
        result.setBugOfflineCount(bugOfflineDOList.size());

        // 逾期任务数



        return BaseResult.success(result);
    }
}

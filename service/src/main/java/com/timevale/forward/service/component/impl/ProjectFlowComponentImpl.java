package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Objects;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectFlowComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.lowcode.support.response.task.TaskHandleUserResponse;
import com.timevale.mandarin.common.query.QueryBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectFlowComponentImpl implements ProjectFlowComponent {
    @Resource
    private EpeiusClient epeiusClient;

    @Resource
    private ProjectFlowMapper projectFlowMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProjectComponent projectComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectLogComponent projectLogComponent;

    @Override
    public void updateFlowInfo(String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            log.info("流程id为空");
            return;
        }
        ProcessResponse processInfo = epeiusClient.getProcessInfo(processInstanceId);
        List<String> currentTaskIdList = processInfo.getCurrentTaskIdList();
        if (CollectionUtils.isEmpty(currentTaskIdList)) {
            log.info("任务id为空");
            return;
        }
        String processStatus = processInfo.getProcessStatus();
        log.info("返回流程信息 processInfo={}", processInfo);
        ProjectFlowDO projectFlowDO = projectFlowMapper.get(null, processInstanceId);
        if (projectFlowDO == null) {
            log.info("无流程数据 flowId={}", processInstanceId);
            return;
        }
        Map<String, Object> flowData = processInfo.getFlowData();
        if (FlowStatusEnum.REJECT.getValue().equals(processStatus)) {
            projectFlowDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.REJECT.getCode());
            String rejectReason = flowData.get("rejectReason") == null ? StringUtils.EMPTY : String.valueOf(flowData.get("rejectReason"));
            projectFlowDO.setReviewFailReason(rejectReason);
            projectFlowDO.setFlowEndDate(new Date());
        } else if (FlowStatusEnum.WITHDRAW.getValue().equals(processStatus)) {
            projectFlowDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.WITHDRAW.getCode());
            projectFlowDO.setFlowEndDate(new Date());
        } else if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            ProjectDO oldProjectDO = projectMapper.get(projectFlowDO.getProjectId());
            projectFlowDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.COMPLETE.getCode());
            projectFlowDO.setFlowEndDate(new Date());
            ProjectNodeDO projectNodeDo = projectNodeMapper.getByName(projectFlowDO.getProjectId(), ProjectNodeEnum.getNameByCode(projectFlowDO.getFlowType()));
            if (projectNodeDo != null) {
                projectNodeMapper.updateActualDateById(projectNodeDo.getId(), projectFlowDO.getReviewDate());
                //更新节点状态
                projectComponent.updateNodeStatus(projectFlowDO.getProjectId());
                projectNodeDo = projectNodeMapper.getByName(projectFlowDO.getProjectId(), ProjectNodeEnum.START_PLAN.getText());
                if (projectNodeDo == null ) {
                    //需求规划阶段被删除,详设评审为第一个节点,需要更新项目实际开始时间
                    ProjectDO updateActualStartDateDO = new ProjectDO();
                    updateActualStartDateDO.setId(projectFlowDO.getProjectId());
                    updateActualStartDateDO.setActualStartDate(processInfo.getEndTime());
                    projectMapper.update(updateActualStartDateDO);
                }
            }

            Integer newStatus = projectComponent.getStatus(projectFlowDO.getProjectId());
            if (!Objects.equal(oldProjectDO.getStatus(), newStatus)
                    && !ProjectStatusEnum.INVALID.getCode().equals(oldProjectDO.getStatus())
                    && !ProjectStatusEnum.SUSPEND.getCode().equals(oldProjectDO.getStatus())) {
                ProjectDO updateStatusDO = new ProjectDO();
                updateStatusDO.setId(projectFlowDO.getProjectId());
                updateStatusDO.setStatus(newStatus);
                projectMapper.update(updateStatusDO);
                projectLogComponent.addLogWhenStatusChange(oldProjectDO.getStatus(), newStatus, oldProjectDO.getId(), null);

            }
        }

        List<String> reviewList = JSONObject.parseArray(projectFlowDO.getReview(), String.class);
        List<String> reviewIdList = JSONObject.parseArray(projectFlowDO.getReviewId(), String.class);

        Map<String, String> reviewMap = new HashMap<>();
        for (int i = 0; i < reviewList.size(); i++) {
            reviewMap.put(reviewIdList.get(i), reviewList.get(i));
        }

        TaskHandleUserResponse taskHandleUserList = epeiusClient.getTaskHandleUserList(currentTaskIdList.get(0));
        log.info("返回人员信息 taskHandleUserList={}", taskHandleUserList);
        List<String> passIds = taskHandleUserList.getPassedUserList().stream().map(TaskHandleUserResponse.TaskUser::getAccountId).collect(Collectors.toList());
        List<String> passAlias = new ArrayList<>();
        passIds.forEach(a -> {
            passAlias.add(reviewMap.get(a));
        });

        List<String> rejectIds = taskHandleUserList.getRejectUserList().stream().map(TaskHandleUserResponse.TaskUser::getAccountId).collect(Collectors.toList());
        List<String> rejectAlias = new ArrayList<>();
        rejectIds.forEach(a -> {
            rejectAlias.add(reviewMap.get(a));
        });

        reviewIdList.removeAll(passIds);
        reviewIdList.removeAll(rejectIds);

        List<String> unReviewAlias = new ArrayList<>();
        reviewIdList.forEach(a -> {
            unReviewAlias.add(reviewMap.get(a));
        });

        projectFlowDO.setReviewedId(CollectionUtils.isEmpty(passIds) ? StringUtils.EMPTY : JSONObject.toJSONString(passIds));
        projectFlowDO.setReviewed(CollectionUtils.isEmpty(passAlias) ? StringUtils.EMPTY : JSONObject.toJSONString(passAlias));
        projectFlowDO.setReviewFailId(CollectionUtils.isEmpty(rejectIds) ? StringUtils.EMPTY : JSONObject.toJSONString(rejectIds));
        projectFlowDO.setReviewFail(CollectionUtils.isEmpty(rejectAlias) ? StringUtils.EMPTY : JSONObject.toJSONString(rejectAlias));
        projectFlowDO.setUnreviewedId(CollectionUtils.isEmpty(reviewIdList) ? StringUtils.EMPTY : JSONObject.toJSONString(reviewIdList));
        projectFlowDO.setUnreviewed(CollectionUtils.isEmpty(unReviewAlias) ? StringUtils.EMPTY : JSONObject.toJSONString(unReviewAlias));
        log.info("更新的数据 projectFlowDO={}", projectFlowDO);
        projectFlowMapper.update(projectFlowDO);
    }

    @Override
    public List<ProjectFlowDO> flushCompleteFlow(QueryBase queryBase) {
        PageHelper.startPage(queryBase.getPageNum(), queryBase.getPageSize());

        List<ProjectFlowDO> projectFlowDOList = projectFlowMapper.pageCompleteFlow();

        PageInfo<ProjectFlowDO> pageInfo = new PageInfo<>(projectFlowDOList);

        if (CollectionUtils.isEmpty(projectFlowDOList)) {
            return projectFlowDOList;
        }

        for (ProjectFlowDO projectFlowDO : projectFlowDOList) {
            Date endTime;

            try {
                ProcessResponse processInfo = epeiusClient.getProcessInfo(projectFlowDO.getFlowId());
                endTime = processInfo.getEndTime();
            } catch (Exception e) {
                //使用更新时间更新
                endTime = projectFlowDO.getModifyDate();
            }

            projectFlowDO.setFlowId(null);
            projectFlowDO.setModifyDate(null);
            projectFlowDO.setFlowEndDate(endTime);
        }

        projectFlowMapper.batchUpdateFlowEndTime(projectFlowDOList);

        return pageInfo.getList();
    }

}

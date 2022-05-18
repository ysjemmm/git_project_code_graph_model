package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSONObject;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.model.enums.ProjectFlowStatusEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectFlowComponent;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.lowcode.support.response.task.TaskHandleUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    public void updateFlowInfo(ProcessResponse processInfo) {
        List<String> currentTaskIdList = processInfo.getCurrentTaskIdList();
        if (CollectionUtils.isEmpty(currentTaskIdList)) {
            log.info("任务id为空");
            return;
        }
        String processStatus = processInfo.getProcessStatus();
        log.info("processInfo={}", processInfo);
        ProjectFlowDO projectFlowDO = projectFlowMapper.get(null, processInfo.getProcessInstanceId());
        if (projectFlowDO == null) {
            log.info("无详设流程, flowId={}", processInfo.getProcessInstanceId());
            return;
        }
        Map<String, Object> flowData = processInfo.getFlowData();
        if (FlowStatusEnum.REJECT.getValue().equals(processStatus)) {
            projectFlowDO.setStatus(ProjectFlowStatusEnum.REVIEW_FAIL.getCode());
        } else if (FlowStatusEnum.WITHDRAW.getValue().equals(processStatus)) {
            projectFlowDO.setStatus(ProjectFlowStatusEnum.WITHDRAW.getCode());
        } else if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            projectFlowDO.setStatus(ProjectFlowStatusEnum.REVIEWED.getCode());
            ProjectNodeDO projectNodeDo = projectNodeMapper.getByName(projectFlowDO.getProjectId(), ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText());
            if (projectNodeDo != null) {
                projectNodeMapper.updateActualDateById(projectNodeDo.getId(), processInfo.getEndTime());
            }
            //更新节点状态
            projectComponent.updateNodeStatus(projectFlowDO.getProjectId());
        }
        String rejectReason = flowData.get("rejectReason") == null ? "" : String.valueOf(flowData.get("rejectReason"));
        projectFlowDO.setReviewFailReason(rejectReason);

        List<String> reviewList = JSONObject.parseArray(projectFlowDO.getReview(), String.class);
        List<String> reviewIdList = JSONObject.parseArray(projectFlowDO.getReviewId(), String.class);

        Map<String, String> reviewMap = new HashMap<>();
        for (int i = 0; i < reviewList.size(); i++) {
            reviewMap.put(reviewIdList.get(i), reviewList.get(i));
        }

        TaskHandleUserResponse taskHandleUserList = epeiusClient.getTaskHandleUserList(currentTaskIdList.get(0));
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

        projectFlowDO.setReviewedId(CollectionUtils.isEmpty(passIds) ? "" : JSONObject.toJSONString(passIds));
        projectFlowDO.setReviewed(CollectionUtils.isEmpty(passAlias) ? "" : JSONObject.toJSONString(passAlias));
        projectFlowDO.setReviewFailId(CollectionUtils.isEmpty(rejectIds) ? "" : JSONObject.toJSONString(rejectIds));
        projectFlowDO.setReviewFail(CollectionUtils.isEmpty(rejectAlias) ? "" : JSONObject.toJSONString(rejectAlias));
        projectFlowDO.setUnreviewedId(CollectionUtils.isEmpty(reviewIdList) ? "" : JSONObject.toJSONString(reviewIdList));
        projectFlowDO.setUnreviewed(CollectionUtils.isEmpty(unReviewAlias) ? "" : JSONObject.toJSONString(unReviewAlias));
        log.info("projectFlowDO={}", projectFlowDO);
        projectFlowMapper.update(projectFlowDO);
    }

}

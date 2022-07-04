package com.timevale.forward.service.impl;

import com.timevale.epeius.service.model.request.TerminateRequest;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectNodeFlowMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import com.timevale.forward.facade.api.client.ProjectNodeFlowService;
import com.timevale.forward.facade.api.request.ProjectNodeFlowCheckReq;
import com.timevale.forward.facade.api.request.ProjectNodeFlowModifyReq;
import com.timevale.forward.facade.api.result.ProjectNodeFlowDetailVO;
import com.timevale.forward.model.enums.ProjectFlowStatusEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.service.component.ProjectNodeFlowComponent;
import com.timevale.forward.service.copy.ProjectNodeCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProjectNodeFlowServiceImpl implements ProjectNodeFlowService {

    @Resource
    private ProjectNodeFlowComponent projectNodeFlowComponent;

    @Resource
    private ProjectNodeFlowMapper projectNodeFlowMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Resource
    private EpeiusClient epeiusClient;

    @Override
    public BaseResult<ProjectNodeFlowDetailVO> get(Long nodeFlowId) {
        log.info("节点审批流程详情,参数:{}", nodeFlowId);
//        ProjectNodeFlowDO oldFlowDo = projectNodeFlowMapper.get(nodeFlowId, null);
//        if (oldFlowDo == null) {
//            throw new BaseBizRuntimeException("找不到该审批流程");
//        }
//        ProjectNodeFlowDetailVO projectFlowDetailVO = ProjectNodeFlowCopier.INSTANCE.convert(oldFlowDo);
//        projectFlowDetailVO.setStatusName(ProjectFlowStatusEnum.getTextByCode(oldFlowDo.getStatus()));
//        List<ProjectNodeFlowDO> projectFlowDos = projectNodeFlowMapper.getByProjectId(oldFlowDo.getProjectId());
//        if(FlowStageEnum.SECOND.getCode().equals(oldFlowDo.getStage())){
//            ProjectNodeFlowDO lastFlowDo = projectNodeFlowMapper.get(nodeFlowId, oldFlowDo.getLastFlowId());
//            projectFlowDetailVO.setReviewFailReason(lastFlowDo.getReviewFailReason());
//            projectFlowDetailVO.setPoReviewFailReason(oldFlowDo.getReviewFailReason());
//        }
//        long changeCount = projectFlowDos.stream().filter(a -> ProjectFlowStatusEnum.REVIEWED.getCode().equals(a.getStatus())).count();
//        projectFlowDetailVO.setChangeCount(changeCount);
        ProjectNodeFlowDetailVO vo = new ProjectNodeFlowDetailVO();
        return BaseResult.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<String> withdraw(ProjectNodeFlowModifyReq projectNodeFlowModifyReq) {
        log.info("节点审批流程撤销,参数:{}", projectNodeFlowModifyReq);
        String flowId = projectNodeFlowModifyReq.getFlowId();
        ProjectNodeFlowDO projectNodeFlowDO = projectNodeFlowMapper.get(null, flowId);
        if (!ProjectFlowStatusEnum.REVIEWING.getCode().equals(projectNodeFlowDO.getStatus())) {
            throw new BaseBizRuntimeException("流程状态非审核中,无法撤销");
        }

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (!Objects.equals(userInfo.getId(), projectNodeFlowDO.getCreateManId())) {
            throw new BaseBizRuntimeException("操作人非流程发起人,无法撤销");
        }

        TerminateRequest request = new TerminateRequest();
        request.setProcessInstanceId(flowId);
        request.setAssignee(userInfo.getId());
        epeiusClient.withdrawInstance(request);

        projectNodeFlowDO.setStatus(ProjectFlowStatusEnum.WITHDRAW.getCode());
        projectNodeFlowMapper.update(projectNodeFlowDO);
        return BaseResult.success();
    }

    @Override
    public BaseResult<Integer> nodeIsDelay(ProjectNodeFlowCheckReq projectNodeFlowCheckReq) {
        log.info("检查节点是否延期,参数:{}", projectNodeFlowCheckReq);
        List<ProjectNodeDO> oldProjectNodes = projectNodeMapper.get(projectNodeFlowCheckReq.getProjectId());
        List<ProjectNodeDO> oldPublishNodes = oldProjectNodes.stream()
                .filter(a -> ProjectNodeEnum.PUBLISH_OFFICIAL.getText().equals(a.getName()) && a.getPlanDate() != null).collect(Collectors.toList());
        List<ProjectNodeDO> oldTestNodes = oldProjectNodes.stream()
                .filter(a -> ProjectNodeEnum.SUBMIT_TEST.getText().equals(a.getName()) && a.getPlanDate() != null).collect(Collectors.toList());

        List<ProjectNodeDO> projectNodes = ProjectNodeCopier.INSTANCE.convert(projectNodeFlowCheckReq.getProjectNodeAddReq());
        List<ProjectNodeDO> publishNodes = projectNodes.stream()
                .filter(a -> ProjectNodeEnum.PUBLISH_OFFICIAL.getText().equals(a.getName()) && a.getPlanDate() != null).collect(Collectors.toList());
        List<ProjectNodeDO> testNodes = projectNodes.stream()
                .filter(a -> ProjectNodeEnum.SUBMIT_TEST.getText().equals(a.getName()) && a.getPlanDate() != null).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(oldPublishNodes) && !CollectionUtils.isEmpty(publishNodes)) {
            Date oldPlanDate = DateUtil.getEndOfDay(oldPublishNodes.get(0).getPlanDate());
            Date planDate = DateUtil.getEndOfDay(publishNodes.get(0).getPlanDate());
            if (oldPlanDate.before(planDate)) {
                Long seconds = elapsedTimeClient.getElapsedTime(oldPlanDate, planDate);
                if (seconds > 0) {
                    return BaseResult.success(1);
                }
            }
        }
        if (!CollectionUtils.isEmpty(oldTestNodes) && !CollectionUtils.isEmpty(testNodes)) {
            Date oldPlanDate = DateUtil.getEndOfDay(oldTestNodes.get(0).getPlanDate());
            Date planDate = DateUtil.getEndOfDay(testNodes.get(0).getPlanDate());
            if (oldPlanDate.before(planDate)) {
                Long seconds = elapsedTimeClient.getElapsedTime(oldPlanDate, planDate);
                if (seconds > 0) {
                    return BaseResult.success(0);
                }
            }
        }
        return BaseResult.success();
    }

}

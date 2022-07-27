package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.timevale.epeius.service.model.request.TerminateRequest;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectNodeFlowMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import com.timevale.forward.facade.api.client.ProjectNodeFlowService;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.request.ProjectNodeFlowCheckReq;
import com.timevale.forward.facade.api.result.ProjectNodeDelayVO;
import com.timevale.forward.facade.api.result.ProjectNodeFlowDetailVO;
import com.timevale.forward.model.enums.FlowStageEnum;
import com.timevale.forward.model.enums.FlowStatusEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.service.component.ProjectNodeFlowComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectNodeCopier;
import com.timevale.forward.service.copy.ProjectNodeFlowCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
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
    private EpeiusClient epeiusClient;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Override
    public BaseResult<ProjectNodeFlowDetailVO> getFlow(Long projectId) {
        log.info("节点审批流程详情,参数:{}", projectId);
        List<ProjectNodeFlowDO> projectFlowDos = projectNodeFlowMapper.getByProjectId(projectId);
        if (CollectionUtils.isEmpty(projectFlowDos)) {
            throw new BaseBizRuntimeException("找不到该审批流程");
        }
        ProjectNodeFlowDO currentFlowDo = projectFlowDos.get(0);
        ProjectNodeFlowDetailVO projectFlowDetailVO = ProjectNodeFlowCopier.INSTANCE.convert(currentFlowDo);
        projectFlowDetailVO.setStatusName(FlowStatusEnum.getTextByCode(currentFlowDo.getStatus()));

        if (!StringUtils.isEmpty(currentFlowDo.getLastFlowId())) {
            //(pd||biz)&&po审批
            ProjectNodeFlowDO lastFlowDo = projectNodeFlowMapper.get(null, currentFlowDo.getLastFlowId());
            projectFlowDetailVO.setReviewFails(JSONObject.parseArray(lastFlowDo.getReviewFail(), String.class));
            projectFlowDetailVO.setReviewFailReason(lastFlowDo.getReviewFailReason());
            projectFlowDetailVO.setPoReviewFailReason(currentFlowDo.getReviewFailReason());
        } else if (FlowStageEnum.SECOND.getCode().equals(currentFlowDo.getStage())) {
            //pd和biz为空,只有po审批
            projectFlowDetailVO.setReviewFails(Lists.emptyList());
            projectFlowDetailVO.setReviewFailReason(StringUtils.EMPTY);
            projectFlowDetailVO.setPoReviewFailReason(currentFlowDo.getReviewFailReason());
        }
        long changeCount = projectFlowDos.stream().filter(a -> FlowStatusEnum.COMPLETE.getCode().equals(a.getStatus())).count();
        projectFlowDetailVO.setChangeCount(changeCount);
        return BaseResult.success(projectFlowDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> withdraw(Long projectId) {
        log.info("节点审批流程撤销,参数:{}", projectId);
        List<ProjectNodeFlowDO> projectFlowDos = projectNodeFlowMapper.getByProjectId(projectId);
        if (CollectionUtils.isEmpty(projectFlowDos)) {
            throw new BaseBizRuntimeException("找不到该审批流程");
        }
        ProjectNodeFlowDO currentFlowDo = projectFlowDos.get(0);
        if (!FlowStatusEnum.AUDITING.getCode().equals(currentFlowDo.getStatus())) {
            throw new BaseBizRuntimeException("流程状态非审核中,无法撤销");
        }


        TerminateRequest request = new TerminateRequest();
        request.setProcessInstanceId(currentFlowDo.getFlowId());
        request.setAssignee(currentFlowDo.getCreateManId());
        epeiusClient.withdrawInstance(request);

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        currentFlowDo.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        currentFlowDo.setModifyManId(userInfo.getId());
        currentFlowDo.setStatus(FlowStatusEnum.WITHDRAW.getCode());
        projectNodeFlowMapper.update(currentFlowDo);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectNodeDelayVO> nodeIsDelay(ProjectNodeFlowCheckReq projectNodeFlowCheckReq) {
        log.info("检查节点是否延期,参数:{}", projectNodeFlowCheckReq);
        ProjectNodeDelayVO delayVO = new ProjectNodeDelayVO();

        List<ProjectNodeDO> oldProjectNodes = projectNodeMapper.get(projectNodeFlowCheckReq.getProjectId());
        List<ProjectNodeDO> oldPublishNodes = oldProjectNodes.stream()
                .filter(a -> ProjectNodeEnum.PUBLISH_OFFICIAL.getText().equals(a.getName()) && a.getPlanDate() != null).collect(Collectors.toList());
        List<ProjectNodeDO> oldTestNodes = oldProjectNodes.stream()
                .filter(a -> ProjectNodeEnum.SUBMIT_TEST.getText().equals(a.getName()) && a.getPlanDate() != null).collect(Collectors.toList());

        List<ProjectNodeDO> projectNodes = ProjectNodeCopier.INSTANCE.convert(projectNodeFlowCheckReq.getProjectNodes());
        List<ProjectNodeDO> publishNodes = projectNodes.stream()
                .filter(a -> ProjectNodeEnum.PUBLISH_OFFICIAL.getText().equals(a.getName()) && a.getPlanDate() != null).collect(Collectors.toList());
        List<ProjectNodeDO> testNodes = projectNodes.stream()
                .filter(a -> ProjectNodeEnum.SUBMIT_TEST.getText().equals(a.getName()) && a.getPlanDate() != null).collect(Collectors.toList());

        Date pjEstablishPublishDate = projectNodeFlowCheckReq.getPjEstablishPublishDate();
        if (pjEstablishPublishDate != null && !CollectionUtils.isEmpty(publishNodes)) {
            Date pjEstablishPublishDateEnd = DateUtil.getEndOfDay(pjEstablishPublishDate);
            Date planDate = DateUtil.getEndOfDay(publishNodes.get(0).getPlanDate());
            if(pjEstablishPublishDateEnd.before(planDate)){
                List<ProjectNodeFlowDO> projectFlowDos = projectNodeFlowMapper.getByProjectId(projectNodeFlowCheckReq.getProjectId());
                boolean match = projectFlowDos.stream().anyMatch(a -> FlowStatusEnum.COMPLETE.getCode().equals(a.getStatus()));
                if (!match) {
                    //有基线版本,且立项预期上线时间小于发布正式计划时间,且无审批通过的流程
                    Long seconds = elapsedTimeClient.getElapsedTime(pjEstablishPublishDateEnd, planDate);
                    BigDecimal elapsedTime = new BigDecimal(seconds.toString());
                    elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
                    delayVO.setDelayDay(elapsedTime);
                    delayVO.setDelayType(2);
                    return BaseResult.success(delayVO);
                }
            }
        }
        if (!CollectionUtils.isEmpty(oldPublishNodes) && !CollectionUtils.isEmpty(publishNodes)) {
            Date oldPlanDate = DateUtil.getEndOfDay(oldPublishNodes.get(0).getPlanDate());
            Date planDate = DateUtil.getEndOfDay(publishNodes.get(0).getPlanDate());
            if (oldPlanDate.before(planDate)) {
                Long seconds = elapsedTimeClient.getElapsedTime(oldPlanDate, planDate);
                BigDecimal elapsedTime = new BigDecimal(seconds.toString());
                elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
                delayVO.setDelayDay(elapsedTime);
                delayVO.setDelayType(1);
                return BaseResult.success(delayVO);
            }
        }
        if (!CollectionUtils.isEmpty(oldTestNodes) && !CollectionUtils.isEmpty(testNodes)) {
            Date oldPlanDate = DateUtil.getEndOfDay(oldTestNodes.get(0).getPlanDate());
            Date planDate = DateUtil.getEndOfDay(testNodes.get(0).getPlanDate());
            if (oldPlanDate.before(planDate)) {
                Long seconds = elapsedTimeClient.getElapsedTime(oldPlanDate, planDate);
                BigDecimal elapsedTime = new BigDecimal(seconds.toString());
                elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
                delayVO.setDelayDay(elapsedTime);
                delayVO.setDelayType(0);
                return BaseResult.success(delayVO);
            }
        }
        delayVO.setDelayDay(BigDecimal.ZERO);
        delayVO.setDelayType(-1);
        return BaseResult.success(delayVO);
    }

    @Override
    public BaseResult<Boolean> test(ProjectModifyReq projectModifyReq) {
        ProjectNodeFlowDO projectNodeFlowDO = ProjectNodeFlowCopier.INSTANCE.convert(projectModifyReq.getProjectNodeFlow());
        List<ProjectNodeDO> projectNodes = ProjectNodeCopier.INSTANCE.convert(projectModifyReq.getProjectNodes());
        projectNodeFlowComponent.process(projectNodeFlowDO, projectNodes);
        return BaseResult.success();
    }

}

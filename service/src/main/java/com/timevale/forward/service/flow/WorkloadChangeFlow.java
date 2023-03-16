package com.timevale.forward.service.flow;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMemberEvaluateMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.dal.entity.ProjectMemberEvaluateDO;
import com.timevale.forward.facade.api.request.MemberWorkloadFillReq;
import com.timevale.forward.facade.api.request.MemberWorkloadModifyReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.ProjectWorkloadChangeVO;
import com.timevale.forward.model.enums.ForwardFlowStatusEnum;
import com.timevale.forward.model.enums.MessageTagEnum;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectEvaluateComponent;
import com.timevale.forward.service.component.UserComponent;
import com.timevale.forward.service.config.CommonConfig;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectEvaluateCopier;
import com.timevale.forward.service.copy.ProjectMemberEvaluateCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.epeius.model.WorkloadChangeVar;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/03/08 18:00
 */
@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class WorkloadChangeFlow {
    final private CommonConfig commonConfig;
    final private EpeiusClient epeiusClient;
    final private UserComponent userComponent;
    final private ProjectMapper projectMapper;
    final private ProjectComponent projectComponent;
    final private ProjectFlowMapper projectFlowMapper;
    final private ProjectEvaluateComponent evaluateComponent;
    final private ProjectMemberEvaluateMapper memberEvaluateMapper;

    /**
     * 工作量变更流程
     *
     * @param changeVO 工作量表单
     * @param req      工作量变更请求
     * @return {@link String}
     */
    @Transactional(rollbackFor = Exception.class)
    public String start(ProjectWorkloadChangeVO changeVO, MemberWorkloadFillReq req) {
        // 当前用户发起人id
        String startAccountId = LocalSessionUtils.getUserInfo().getId();

        // 查询结项流程PMO
        List<String> pmoIdList = userComponent.getPmo(commonConfig.getEvalPmoGroup());

        // 取出项目id、变更事由、PBU负责人
        final Long projectId = req.getProjectId();
        final String changeReason = req.getChangeReason();
        PersonAddReq pbuPrincipal = req.getPbuPrincipal();

        // 校验参数
        AssertUtil.notBlank(changeReason, "变更事由不能为空");
        if (ProjectKindEnum.PBG_OTN.getCode().equals(changeVO.getKind())) {
            pbuPrincipal = new PersonAddReq();
        } else {
            AssertUtil.notNull(pbuPrincipal, "PBU负责人不能为空");
        }

        // 配置项目信息
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");
        final String srId = projectDO.getSrId();
        final String projectUrl = projectComponent.getUrl(projectId);

        // 发起人是否为项目负责人或者1-n产研负责人
        Set<String> principalIdSet = CollUtil.newHashSet(projectDO.getPrincipalId(), projectDO.getOtnPrincipalId());
        List<String> principalIdList = principalIdSet.stream().filter(StrUtil::isNotBlank).collect(Collectors.toList());
        boolean startIsPrincipal = principalIdSet.contains(startAccountId);
        String isPrincipal = YesOrNoEnum.getTextByCode(startIsPrincipal);

        // 项目参数填装
        WorkloadChangeVar changeVar = ProjectEvaluateCopier.INSTANCE.vo2var(changeVO);
        changeVar.setSrId(srId);
        changeVar.setPMOIdList(pmoIdList);
        changeVar.setProjectUrl(projectUrl);
        changeVar.setIsPrincipal(isPrincipal);
        changeVar.setChangeReason(changeReason);
        changeVar.setPrincipalIdList(principalIdList);
        changeVar.setPbuPrincipal(pbuPrincipal.getUserName());
        changeVar.setPbuPrincipalId(pbuPrincipal.getUserId());

        // 转换为Map
        Map<String, Object> variables = BeanUtil.beanToMap(changeVar);

        // 流程参数填装
        StartProcessRequest startProcessReq = new StartProcessRequest();
        startProcessReq.setVariables(variables);
        startProcessReq.setStartAccountId(startAccountId);
        startProcessReq.setEpeVirtualProcessSwitch(false);
        startProcessReq.setApplicationName(CommonConstant.APP);
        startProcessReq.setProcessDefinitionKey(MessageTagEnum.FORWARD_WORKLOAD_CHANGE.getText());

        return epeiusClient.start(startProcessReq);
    }

    /**
     * 工作量变更——工作流完成回调处理
     *
     * @param processInstanceId 流程实例id
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(String processInstanceId) {
        AssertUtil.checkState(StrUtil.isNotBlank(processInstanceId), "流程id为空");

        // 获取项目流程信息
        ProjectFlowDO projectFlowDO = projectFlowMapper.getByFlowId(processInstanceId);
        AssertUtil.notNull(projectFlowDO, "流程不存在");

        // 判断项目流程状态
        AssertUtil.checkState(ForwardFlowStatusEnum.AUDITING.getCode().equals(projectFlowDO.getStatus()), "流程已结束");

        // 配置用户信息
        LocalSessionUtils.setUserInfo(projectFlowDO.getProposerId(), projectFlowDO.getProposer());

        // 获取流程信息
        ProcessResponse processInfo  = epeiusClient.getProcessInfo(processInstanceId);
        AssertUtil.notNull(processInfo, "流程信息为空");

        // 更新流程状态
        String processStatus = processInfo.getProcessStatus();
        Optional.ofNullable(ForwardFlowStatusEnum.getByValue(processStatus))
                .flatMap(e -> Optional.ofNullable(e.getCode()))
                .ifPresent(e -> projectFlowMapper.updateStatus(projectFlowDO.getId(), e));

        // 只有审批通过，需要更新项目工作量信息
        if (ObjectUtil.notEqual(FlowStatusEnum.FLOW_COMPLETE.getValue(), processStatus)) {
            return;
        }

        // 项目id
        final Long projectId = projectFlowDO.getProjectId();

        // 解析对应工作流的数据
        String flowData = projectFlowDO.getFlowData();
        List<MemberWorkloadModifyReq> modifyReqList = JSON.parseArray(flowData, MemberWorkloadModifyReq.class);

        // 需要更新的员工id set
        Set<String> recordUserIdSet = modifyReqList.stream().map(MemberWorkloadModifyReq::getUserId).collect(Collectors.toSet());

        // 更新当前成员工作量
        for (MemberWorkloadModifyReq modifyReq : modifyReqList) {
            ProjectMemberEvaluateDO evaluateDO = ProjectMemberEvaluateCopier.INSTANCE.req2do(modifyReq, projectId);
            memberEvaluateMapper.update(evaluateDO);
        }

        // 生成版本
        evaluateComponent.additionRecord(projectId, recordUserIdSet);
    }
}

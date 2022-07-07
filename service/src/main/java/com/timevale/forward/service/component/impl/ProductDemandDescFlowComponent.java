package com.timevale.forward.service.component.impl;

import cn.hutool.http.HtmlUtil;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.request.ProductDemandDescChangeReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.config.CommonConfig;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProductDemandDescFlowCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jingchun
 * create on 2022/7/5
 */
@Slf4j
@Component
public class ProductDemandDescFlowComponent {


    @Resource
    private EpeiusClient epeiusClient;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private ProductDemandDescRecordMapper productDemandDescRecordMapper;

    @Resource
    private ProductDemandDescFlowMapper productDemandDescFlowMapper;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private CommonConfig config;

    public void startProductDemandDescChangeFlow(ProductDemandModifyReq productDemandModifyReq) {
        ProductDemandDescChangeReq descChangeReq = productDemandModifyReq.getDescChangeReq();
        if (descChangeReq == null) {
            return;
        }
        ProductDemandDO productDemand = productDemandMapper.get(productDemandModifyReq.getId());
        AssertUtil.checkState(ProductDemandStatusEnum.PROGRESS.getCode().equals(productDemand.getStatus()),
                "只有项目在进行中时才可以发起需求变更流程");
        ProjectProductDemandDO projectProduct = projectProductDemandMapper.getByProductDemandId(productDemand.getId());
        ProjectDO project = projectMapper.get(projectProduct.getProjectId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String userAlias = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();

        List<BaseInfoResponse> persons =
                innerUserPersonClient.getPersonByAccountNew(Collections.singletonList(descChangeReq.getPoId()));
        AssertUtil.notEmpty(persons, "po用户不存在，请检查参数");
        BaseInfoResponse po = persons.get(0);
        String poName = po.getAlias() + CommonConstant.JOIN_LINE + po.getName();

        Map<String, Object> variables = new HashMap<>();
        variables.put("createMan", userAlias);
        variables.put("productDemandStatus", ProductDemandStatusEnum.getTextByCode(productDemand.getStatus()));
        variables.put("projectName", project.getName());
        variables.put("changeType",
                ProductDemandDescChangeTypeEnum.getTextByCode(descChangeReq.getProductDemandDescChangeType()));
        variables.put("reason", descChangeReq.getReason());
        variables.put("previousDesc", StringEscapeUtils.unescapeHtml(HtmlUtil.cleanHtmlTag(productDemand.getDesc())));
        variables.put("changeDesc", StringEscapeUtils.unescapeHtml(HtmlUtil.cleanHtmlTag(descChangeReq.getChangeDesc())));
        variables.put("pm", project.getPmName());
        variables.put("auditUserId", project.getPmId());
        variables.put("poId", descChangeReq.getPoId());
        variables.put("po", poName);
        variables.put("detailLink", config.getProductManagementViewUrl() + productDemand.getId());
        Integer changeTimes = productDemandDescRecordMapper.countByProductDemandId(productDemand.getId());
        variables.put("descChangeTimes", changeTimes == 0 ? changeTimes : changeTimes - 1);
        String flowId = startFlow(variables, userInfo.getId());

        // 插入流程记录
        ProductDemandDescFlowDO flow = new ProductDemandDescFlowDO()
                .setStage(FlowStageEnum.FIRST.getCode())
                .setFlowId(flowId)
                .setLastFlowId(StringUtils.EMPTY)
                .setReviewFailReason(StringUtils.EMPTY)
                .setProductDemandId(productDemand.getId())
                .setDesc(productDemand.getDesc())
                .setReason(descChangeReq.getReason())
                .setChangeDesc(descChangeReq.getChangeDesc())
                .setChangeType(descChangeReq.getProductDemandDescChangeType())
                .setPmId(project.getPmId())
                .setPm(project.getPmName())
                .setPoId(po.getAccount())
                .setPo(poName);
        flow.setCreateManId(userInfo.getId());
        flow.setCreateMan(userAlias);

        productDemandDescFlowMapper.insert(flow);
    }

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
        ProductDemandDescFlowDO auditingFlow = productDemandDescFlowMapper.getAuditingByFlowId(processInstanceId);
        if (auditingFlow == null) {
            log.info("无审批中产品需求变更流程");
            return;
        }
        Map<String, Object> flowData = processInfo.getFlowData();
        if (FlowStatusEnum.REJECT.getValue().equals(processStatus)) {
            auditingFlow.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.REJECT.getCode());
            String rejectReason = flowData.get("rejectReason") == null ? StringUtils.EMPTY : String.valueOf(flowData.get("rejectReason"));
            auditingFlow.setReviewFailReason(rejectReason);
            if (FlowStageEnum.FIRST.getCode().equals(auditingFlow.getStage())) {
                // 第一阶段拒绝，发起po审核流程
                flowData.put("pm2", flowData.get("pm"));
                flowData.put("pmResult", "拒绝");
                flowData.put("reviewFailReason", rejectReason);
                flowData.put("auditUserId", flowData.get("poId"));
                String newFlowId = startFlow(flowData, auditingFlow.getCreateManId());
                ProductDemandDescFlowDO newFlow = ProductDemandDescFlowCopier.INSTANCE.clone(auditingFlow);
                newFlow.setFlowId(newFlowId)
                        .setStatus(com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode())
                        .setLastFlowId(auditingFlow.getFlowId())
                        .setReviewFailReason(StringUtils.EMPTY)
                        .setStage(FlowStageEnum.SECOND.getCode());
                productDemandDescFlowMapper.insert(newFlow);
            }
        } else if (FlowStatusEnum.WITHDRAW.getValue().equals(processStatus)) {
            auditingFlow.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.WITHDRAW.getCode());
        } else if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            auditingFlow.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.COMPLETE.getCode());
            // 审批成功
            Integer count = productDemandDescRecordMapper.countByProductDemandId(auditingFlow.getProductDemandId());
            ProductDemandDO productDemand = productDemandMapper.get(auditingFlow.getProductDemandId());
            if (count == 0) {
                // 首次变更，创建1.0版本
                ProductDemandDescRecordDO oldProductDemandDesc = new ProductDemandDescRecordDO()
                        .setProductDemandId(productDemand.getId())
                        .setDesc(productDemand.getDesc())
                        .setVersion(BigDecimal.ONE);
                oldProductDemandDesc.setCreateManId(productDemand.getModifyManId());
                oldProductDemandDesc.setCreateMan(productDemand.getModifyMan());
                productDemandDescRecordMapper.insert(oldProductDemandDesc);
                count = 1;
            }
            // 创建新版本
            ProductDemandDescRecordDO newProductDemandDesc = new ProductDemandDescRecordDO()
                    .setProductDemandId(productDemand.getId())
                    .setDesc(auditingFlow.getChangeDesc())
                    .setVersion(BigDecimal.valueOf(count + 1));
            newProductDemandDesc.setCreateManId(auditingFlow.getCreateManId());
            newProductDemandDesc.setCreateMan(auditingFlow.getCreateMan());
            productDemandDescRecordMapper.insert(newProductDemandDesc);
            productDemand.setDesc(auditingFlow.getChangeDesc());
            productDemandMapper.update(productDemand);
            // 创建变更记录
            BizChangeLogDO bizChangeLogDO = new BizChangeLogDO()
                    .setMainId(productDemand.getId())
                    .setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode())
                    .setField(BizChangeLogFieldEnum.DESC.getText())
                    .setOldValue(StringEscapeUtils.unescapeHtml(HtmlUtil.cleanHtmlTag(auditingFlow.getDesc())))
                    .setNewValue(StringEscapeUtils.unescapeHtml(HtmlUtil.cleanHtmlTag(auditingFlow.getChangeDesc())));
            bizChangeLogDO.setCreateManId(auditingFlow.getCreateManId());
            bizChangeLogDO.setCreateMan(auditingFlow.getCreateMan());
            bizChangeLogDO.setContent(String.format("{\"taskId\": \"%s\"}", currentTaskIdList.get(0)));
            bizChangeLogMapper.insert(bizChangeLogDO);
        }
        productDemandDescFlowMapper.update(auditingFlow);

    }

    private String startFlow(Map<String, Object> variables, String startAccount) {
        StartProcessRequest start = new StartProcessRequest();
        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_productDemandChange");
        start.setStartAccountId(startAccount);
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        return epeiusClient.start(start);
    }

}

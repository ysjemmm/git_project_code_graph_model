package com.timevale.forward.service.component.impl;

import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandDescFlowDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.facade.api.request.ProductDemandDescChangeReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.model.enums.ProductDemandDescChangeTypeEnum;
import com.timevale.forward.model.enums.ProductDemandDescFlowStageEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
        variables.put("previousDesc", productDemand.getDesc());
        variables.put("changeDesc", descChangeReq.getChangeDesc());
        variables.put("pm", project.getPmName());
        variables.put("auditUserId", project.getPmId());
        variables.put("poId", descChangeReq.getPoId());
        variables.put("po", poName);
        variables.put("descChangeTimes", productDemandDescRecordMapper.countByProductDemandId(productDemand.getId()));
        String flowId = startFlow(variables);

        // 插入流程记录
        ProductDemandDescFlowDO flow = new ProductDemandDescFlowDO()
                .setStage(ProductDemandDescFlowStageEnum.FIRST_STAGE.getCode())
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

    private String startFlow(Map<String, Object> variables) {
        StartProcessRequest start = new StartProcessRequest();
        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_productDemandChange");
        start.setStartAccountId("jingchun");
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        return epeiusClient.start(start);
    }

}

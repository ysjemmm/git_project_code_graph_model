package com.timevale.forward.service.impl;

import com.timevale.epeius.service.model.request.TerminateRequest;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandDescFlowMapper;
import com.timevale.forward.dal.entity.ProductDemandDescFlowDO;
import com.timevale.forward.facade.api.client.ProductDemandDescFlowService;
import com.timevale.forward.facade.api.request.ProductDemandIdReq;
import com.timevale.forward.facade.api.result.ProductDemandDescFlowVO;
import com.timevale.forward.model.enums.FlowStatusEnum;
import com.timevale.forward.service.copy.ProductDemandDescFlowCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * 产品需求变更流程接口
 *
 * @author jingchun
 * create on 2022/7/4
 */
@Slf4j
@RestService
public class ProductDemandDescFlowServiceImpl implements ProductDemandDescFlowService {

    @Resource
    private ProductDemandDescFlowMapper productDemandDescFlowMapper;

    @Resource
    private EpeiusClient epeiusClient;

    @Override
    public BaseResult<ProductDemandDescFlowVO> getLatestDescFlow(Long productDemandId) {
        ProductDemandDescFlowDO flow = productDemandDescFlowMapper.getLastByProductDemandId(productDemandId);
        if (flow == null) {
            return BaseResult.success();
        }
        ProductDemandDescFlowVO res = ProductDemandDescFlowCopier.INSTANCE.convert(flow);
        ProcessResponse processInfo = epeiusClient.getProcessInfo(flow.getFlowId());
        List<String> tasks = processInfo.getCurrentTaskIdList();
        if (tasks.isEmpty()) {
            return BaseResult.success(res);
        }
        res.setTaskId(tasks.get(0));
        return BaseResult.success(res);
    }

    @Override
    public BaseResult<Boolean> withdrawProductDemandDescFlow(ProductDemandIdReq productDemandId) {
        ProductDemandDescFlowDO flow = productDemandDescFlowMapper.getLastByProductDemandId(productDemandId.getProductDemandId());
        AssertUtil.notNull(flow, "该产品需求不存在流程变更记录，无法撤回流程");
        AssertUtil.checkState(FlowStatusEnum.AUDITING.getCode().equals(flow.getStatus()), "该审批流程处于" +
                FlowStatusEnum.getTextByCode(flow.getStatus()) + "状态，无法撤回");
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        AssertUtil.checkState(userInfo.getId().equals(flow.getCreateManId()), "该流程不是您发起的，无法撤回");
        TerminateRequest terminateRequest = new TerminateRequest();
        terminateRequest.setProcessInstanceId(flow.getFlowId());
        terminateRequest.setAssignee(userInfo.getId());
        epeiusClient.withdrawInstance(terminateRequest);
        flow.setStatus(FlowStatusEnum.WITHDRAW.getCode());
        productDemandDescFlowMapper.update(flow);
        return BaseResult.success(true);
    }
}

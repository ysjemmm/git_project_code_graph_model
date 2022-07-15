package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.CustomDemandListCondition;
import com.timevale.forward.dal.dao.CustomDemandMapper;
import com.timevale.forward.dal.dao.ProductCustomDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.dal.entity.ProductCustomDemandDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import com.timevale.forward.model.bo.ProductEndBO;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ProblemTypeEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.component.CustomDemandComponent;
import com.timevale.forward.service.copy.CustomDemandCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class CustomDeamndComponentImpl implements CustomDemandComponent {


    @Resource
    private CustomDemandMapper customDemandMapper;

    @Resource
    private ProductCustomDemandMapper productCustomDemandMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Value("${custom.demand.receiver}")
    private String receiver;

    @Override
    public BaseResult<PageQueryResult<CustomDemandVO>> list(CustomDemandListCondition condition) {
        // 分页数据
        buildConditionBeforeQuery(condition);
        List<CustomDemandDO> customDemandDOList;
        if(!condition.isLinkCustomList()){
            customDemandDOList = customDemandMapper.list(condition);
        }else {
            customDemandDOList = customDemandMapper.linkCustomDemandList(condition.getProductDemandId());
        }
        List<CustomDemandVO> customDemandVOList = CustomDemandCopier.INSTANCE.convert(customDemandDOList);

        List<ProductEndBO> receivers = JSONObject.parseArray(receiver, ProductEndBO.class);
        Map<Integer, ProductEndBO> receiverMap = receivers.stream().collect(Collectors.toMap(ProductEndBO::getCode, k -> k, (v1, v2) -> v2));
        customDemandVOList.forEach(a->{
            a.setProductEndText(receiverMap.get(a.getProductEnd()).getName());
            a.setStatusText(BizDemandStatusEnum.getTextByCode(a.getStatus()));
            a.setCauseText(ProblemTypeEnum.getTextByCode(a.getCause()));
        });

        PageInfo<CustomDemandDO> pageInfo = new PageInfo<>(customDemandDOList);
        PageQueryResult<CustomDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(customDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public void updateStatusBaseOnProductDemand(Long customDemandId) {
        List<Long> productDemandIdList = productCustomDemandMapper.getByCustomDemandId(customDemandId)
                .stream().map(ProductCustomDemandDO::getProductDemandId).collect(Collectors.toList());

        List<ProductDemandDO> productDemandDOList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(productDemandIdList)) {
            productDemandDOList = productDemandMapper.selectByIdList(productDemandIdList);
        }

        // 筛出最小产品需求状态
        Integer status = null;
        for (ProductDemandDO productDemandDO : productDemandDOList) {
            Integer productDemandStatus = productDemandDO.getStatus();
            if (productDemandStatus.equals(ProductDemandStatusEnum.INVALID.getCode())) {
                continue;
            }
            status = status == null ? productDemandStatus : Math.min(status, productDemandStatus);
        }

        // 根据产品需求状态判断业务需求状态
        int newStatus;
        if (ProductDemandStatusEnum.INCLUDED.getCode().equals(status)) {
            newStatus = BizDemandStatusEnum.INCLUDE_PROJECT.getCode();
        } else if (ProductDemandStatusEnum.PROGRESS.getCode().equals(status)) {
            newStatus = BizDemandStatusEnum.PROJECTING.getCode();
        } else if (ProductDemandStatusEnum.ONLINE.getCode().equals(status)) {
            newStatus = BizDemandStatusEnum.AVAILABLE.getCode();
        } else if (ProductDemandStatusEnum.WAITING.getCode().equals(status) || ProductDemandStatusEnum.SUSPEND.getCode().equals(status)) {
            newStatus = BizDemandStatusEnum.PD_LINKED.getCode();
        } else {
            newStatus = BizDemandStatusEnum.RECEIVED.getCode();
        }

        // 判断状态是否发生变更
        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandId);
        Integer oldStatus = customDemandDO.getStatus();
        if (!Objects.equals(oldStatus, newStatus) && !Objects.equals(BizDemandStatusEnum.REJECT.getCode(), oldStatus)) {
            // 状态更新
            CustomDemandDO newCustomDemand = new CustomDemandDO();
            newCustomDemand.setId(customDemandId);
            newCustomDemand.setStatus(newStatus);
            customDemandMapper.update(newCustomDemand);
        }
    }
    private void buildConditionBeforeQuery(CustomDemandListCondition condition) {
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        condition.setProjectEndDateStart(DateUtil.getStartOfDay(condition.getProjectEndDateStart()));
        condition.setProjectEndDateEnd(DateUtil.getEndOfDay(condition.getProjectEndDateEnd()));
    }

}

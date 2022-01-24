package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.service.component.ProductBizDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProductBizDemandComponentImpl implements ProductBizDemandComponent {

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Override
    public void update(ProductBizDemandDO productBizDemandDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        productBizDemandDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        productBizDemandDO.setModifyManId(userInfo.getId());
        productBizDemandMapper.update(productBizDemandDO);
    }

    @Override
    public void batchInsert(Long productDemandId, List<Long> bizDemandIds) {
        if(CollectionUtils.isEmpty(bizDemandIds)){
            return;
        }
        List<ProductBizDemandDO> exists = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .productDemandId(productDemandId)
                .isDeleted(false)
                .build());
        List<Long> existBizDemandIds = exists.stream().map(ProductBizDemandDO::getBizDemandId)
                .collect(Collectors.toList());
        log.info("关联业务需求,existBizDemandIds={}", existBizDemandIds);
        bizDemandIds.removeAll(existBizDemandIds);
        if (!CollectionUtils.isEmpty(bizDemandIds)) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            Set<Long> set = new HashSet<>(bizDemandIds);
            List<ProductBizDemandDO> list = set.stream().map(i -> {
                ProductBizDemandDO productDemandDO = new ProductBizDemandDO();
                productDemandDO.setProductDemandId(productDemandId);
                productDemandDO.setBizDemandId(i);
                productDemandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
                productDemandDO.setCreateManId(userInfo.getId());
                return productDemandDO;
            }).collect(Collectors.toList());
            productBizDemandMapper.batchInsert(list);
        }
    }
}

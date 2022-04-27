package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.ProductBizDemandComponent;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
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
public class ProductBizDemandComponentImpl implements ProductBizDemandComponent {

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Resource
    private BizDemandLogComponent bizDemandLogComponent;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Override
    public void update(Long productDemandId, Long bizDemandId) {
        log.info("删除产品与业务需求关系,productDemandId={},bizDemandId={}", productDemandId, bizDemandId);
        // unlink before
        List<Long> bizDemandIds = new ArrayList<>();
        if (bizDemandId != null) {
            //产品与业务需求删除关联
            bizDemandIds.add(bizDemandId);
        } else {
            //产品需求作废
            bizDemandIds = productBizDemandMapper.getByProductDemandIds(Lists.newArrayList(productDemandId))
                    .stream().map(ProductBizDemandDO::getBizDemandId).distinct().collect(Collectors.toList());
        }
        Map<Long, Date> publishDateMap = new HashMap<>();
        before(publishDateMap, bizDemandIds);
        log.info("bizDemandId,publishDate:{}", publishDateMap);
        // unlink
        ProductBizDemandDO productDemandDO = new ProductBizDemandDO();
        productDemandDO.setIsDeleted(true);
        productDemandDO.setProductDemandId(productDemandId);
        productDemandDO.setBizDemandId(bizDemandId);
        productBizDemandMapper.update(productDemandDO);
        // unlink after
        after(publishDateMap, bizDemandIds);
    }

    @Override
    public void batchInsert(Long productDemandId, List<Long> bizDemandIds) {
        log.info("新增产品与业务需求关系,productDemandId={},bizDemandId={}", productDemandId, bizDemandIds);
        if (CollectionUtils.isEmpty(bizDemandIds)) {
            return;
        }
        ProductBizDemandCondition c = ProductBizDemandCondition.builder().productDemandId(productDemandId).isDeleted(false).build();
        List<ProductBizDemandDO> exists = productBizDemandMapper.select(c);
        List<Long> existBizDemandIds = exists.stream().map(ProductBizDemandDO::getBizDemandId).collect(Collectors.toList());
        bizDemandIds.removeAll(existBizDemandIds);
        // link before
        Map<Long, Date> publishDateMap = new HashMap<>();
        before(publishDateMap, bizDemandIds);
        log.info("bizDemandId,publishDate:{}", publishDateMap);
        // link
        if (!CollectionUtils.isEmpty(bizDemandIds)) {
            Set<Long> set = new HashSet<>(bizDemandIds);
            List<ProductBizDemandDO> list = set.stream().map(i -> {
                ProductBizDemandDO productDemandDO = new ProductBizDemandDO();
                productDemandDO.setProductDemandId(productDemandId);
                productDemandDO.setBizDemandId(i);
                return productDemandDO;
            }).collect(Collectors.toList());
            productBizDemandMapper.batchInsert(list);
        }
        // link after
        after(publishDateMap, bizDemandIds);
    }

    private void before(Map<Long, Date> publishDateMap, List<Long> bizDemandIds) {
        if (!CollectionUtils.isEmpty(bizDemandIds)) {
            bizDemandIds.forEach(bid -> {
                Date publishDate = bizDemandComponent.getProjectEndDate(bid);
                publishDateMap.put(bid, publishDate);
            });
        }
    }

    private void after(Map<Long, Date> publishDateMap, List<Long> bizDemandIds) {
        List<BizChangeLogDO> logs = new ArrayList<>();
        bizDemandIds.forEach(bid -> {
            Date publishDate = bizDemandComponent.getProjectEndDate(bid);
            if (!Objects.equals(publishDateMap.get(bid), publishDate)) {
                String oldValue = DateUtil.parseToString(publishDateMap.get(bid), DateStyle.YYYY_MM_DD);
                String newValue = DateUtil.parseToString(publishDate, DateStyle.YYYY_MM_DD);
                logs.add(bizDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, bid));
            }
        });
        if (CollectionUtils.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }
}

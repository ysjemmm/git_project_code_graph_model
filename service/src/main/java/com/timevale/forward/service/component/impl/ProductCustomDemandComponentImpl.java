package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProductCustomDemandCondition;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductCustomDemandMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.ProductCustomDemandDO;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.ProductCustomDemandComponent;
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
public class ProductCustomDemandComponentImpl implements ProductCustomDemandComponent {

    @Resource
    private ProductCustomDemandMapper productCustomDemandMapper;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Resource
    private BizDemandLogComponent bizDemandLogComponent;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;


    @Override
    public void update(Long productDemandId, Long customDemandId) {
        log.info("删除产品与业务需求关系:{},{}", productDemandId, customDemandId);
        // unlink before
        List<Long> customDemandIds = new ArrayList<>();
        if (customDemandId != null) {
            //产品与业务需求删除关联
            customDemandIds.add(customDemandId);
        } else {
            //产品需求作废
            customDemandIds = productCustomDemandMapper.getByProductDemandIds(Lists.newArrayList(productDemandId))
                    .stream().map(ProductCustomDemandDO::getCustomDemandId).distinct().collect(Collectors.toList());
        }
        Map<Long, Date> publishDateMap = new HashMap<>();
//        before(publishDateMap, customDemandIds);
        log.info("customDemandId,publishDate:{}", publishDateMap);
        // unlink
        ProductCustomDemandDO customDemandDO = new ProductCustomDemandDO();
        customDemandDO.setIsDeleted(true);
        customDemandDO.setProductDemandId(productDemandId);
        customDemandDO.setCustomDemandId(customDemandId);
        productCustomDemandMapper.update(customDemandDO);
        // unlink after
//        after(publishDateMap, customDemandIds);
    }

    @Override
    public void batchInsert(Long productDemandId, List<Long> customDemandIds) {
        log.info("产品需求详情,新增关联关系:{},{}", productDemandId, customDemandIds);
        if (CollectionUtils.isEmpty(customDemandIds)) {
            return;
        }
        ProductCustomDemandCondition c = ProductCustomDemandCondition.builder().productDemandId(productDemandId).isDeleted(false).build();
        List<ProductCustomDemandDO> exists = productCustomDemandMapper.select(c);
        List<Long> existCustomDemandIds = exists.stream().map(ProductCustomDemandDO::getCustomDemandId).collect(Collectors.toList());
        customDemandIds.removeAll(existCustomDemandIds);
        // link before
        Map<Long, Date> publishDateMap = new HashMap<>();
//        before(publishDateMap, customDemandIds);
        log.info("customDemandId,publishDate:{}", publishDateMap);
        // link
        if (!CollectionUtils.isEmpty(customDemandIds)) {
            Set<Long> set = new HashSet<>(customDemandIds);
            List<ProductCustomDemandDO> list = set.stream().map(i -> {
                ProductCustomDemandDO customDemandDO = new ProductCustomDemandDO();
                customDemandDO.setProductDemandId(productDemandId);
                customDemandDO.setCustomDemandId(i);
                return customDemandDO;
            }).collect(Collectors.toList());
            productCustomDemandMapper.batchInsert(list);
        }
        // link after
//        after(publishDateMap, customDemandIds);
    }

    @Override
    public void batchInsert(List<Long> productDemandIds,Long customDemandId) {
        log.info("客户需求详情,新增关联关系:{},{}", productDemandIds, customDemandId);
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return;
        }
        ProductCustomDemandCondition c = ProductCustomDemandCondition.builder().customDemandId(customDemandId).isDeleted(false).build();
        List<ProductCustomDemandDO> exists = productCustomDemandMapper.select(c);
        List<Long> existProductDemandIds = exists.stream().map(ProductCustomDemandDO::getProductDemandId).collect(Collectors.toList());
        productDemandIds.removeAll(existProductDemandIds);
        // link before
        Map<Long, Date> publishDateMap = new HashMap<>();
//        before(publishDateMap, customDemandIds);
        log.info("customDemandId,publishDate:{}", publishDateMap);
        // link
        if (!CollectionUtils.isEmpty(existProductDemandIds)) {
            Set<Long> set = new HashSet<>(existProductDemandIds);
            List<ProductCustomDemandDO> list = set.stream().map(i -> {
                ProductCustomDemandDO customDemandDO = new ProductCustomDemandDO();
                customDemandDO.setProductDemandId(i);
                customDemandDO.setCustomDemandId(customDemandId);
                return customDemandDO;
            }).collect(Collectors.toList());
            productCustomDemandMapper.batchInsert(list);
        }
        // link after
//        after(publishDateMap, customDemandIds);
    }

    private void before(Map<Long, Date> publishDateMap, List<Long> customDemandIds) {
        if (!CollectionUtils.isEmpty(customDemandIds)) {
            customDemandIds.forEach(bid -> {
                Date publishDate = bizDemandComponent.getProjectEndDate(bid);
                publishDateMap.put(bid, publishDate);
            });
        }
    }

    private void after(Map<Long, Date> publishDateMap, List<Long> bizDemandIds) {
        List<BizChangeLogDO> logs = new ArrayList<>();
        bizDemandIds.forEach(bid -> {
            bizDemandComponent.updateProjectEndDate(bid);
            BizDemandDO bizDemandDO = bizDemandMapper.selectById(bid);
            if (!Objects.equals(publishDateMap.get(bid), bizDemandDO.getProjectEndDate())) {
                String oldValue = DateUtil.parseToString(publishDateMap.get(bid), DateStyle.YYYY_MM_DD);
                String newValue = DateUtil.parseToString(bizDemandDO.getProjectEndDate(), DateStyle.YYYY_MM_DD);
                logs.add(bizDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, bid));
            }
        });
        if (CollectionUtils.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }
}

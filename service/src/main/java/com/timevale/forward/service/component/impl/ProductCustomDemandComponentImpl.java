package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProductCustomDemandCondition;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.CustomDemandMapper;
import com.timevale.forward.dal.dao.ProductCustomDemandMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.dal.entity.ProductCustomDemandDO;
import com.timevale.forward.service.component.CustomDemandComponent;
import com.timevale.forward.service.component.CustomDemandLogComponent;
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
    private CustomDemandComponent customDemandComponent;

    @Resource
    private CustomDemandLogComponent customDemandLogComponent;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private CustomDemandMapper customDemandMapper;


    @Override
    public void update(Long productDemandId, Long customDemandId,boolean updatePublishDate) {
        log.info("删除产品与客户需求关系:{},{},{}", productDemandId, customDemandId,updatePublishDate);
        // unlink before
        List<Long> customDemandIds = new ArrayList<>();
        if (customDemandId != null) {
            //产品与客户需求删除关联
            customDemandIds.add(customDemandId);
        } else {
            //产品需求作废
            customDemandIds = productCustomDemandMapper.selectByProductDemandIds(Lists.newArrayList(productDemandId))
                    .stream().map(ProductCustomDemandDO::getCustomDemandId).distinct().collect(Collectors.toList());
        }
        Map<Long, Date> publishDateMap = new HashMap<>();
        if(updatePublishDate){
            before(publishDateMap, customDemandIds);
            log.info("customDemandId,publishDate:{}", publishDateMap);
        }
        // unlink
        ProductCustomDemandDO customDemandDO = new ProductCustomDemandDO();
        customDemandDO.setIsDeleted(true);
        customDemandDO.setProductDemandId(productDemandId);
        customDemandDO.setCustomDemandId(customDemandId);
        productCustomDemandMapper.update(customDemandDO);
        // unlink after
        if(updatePublishDate){
            after(publishDateMap, customDemandIds);
        }
    }

    @Override
    public void batchInsert(Long productDemandId, List<Long> customDemandIds,boolean updatePublishDate) {
        log.info("产品需求详情,新增关联关系:{},{}", productDemandId, customDemandIds);
        if (CollectionUtils.isEmpty(customDemandIds)) {
            return;
        }
        ProductCustomDemandCondition c = ProductCustomDemandCondition.builder().productDemandId(productDemandId).isDeleted(false).build();
        List<ProductCustomDemandDO> exists = productCustomDemandMapper.select(c);
        List<Long> existCustomDemandIds = exists.stream().map(ProductCustomDemandDO::getCustomDemandId).collect(Collectors.toList());
        customDemandIds.removeAll(existCustomDemandIds);
        if (CollectionUtils.isEmpty(customDemandIds)) {
            return;
        }
        // link before
        Map<Long, Date> publishDateMap = new HashMap<>();
        if(updatePublishDate){
            before(publishDateMap, customDemandIds);
            log.info("customDemandId,publishDate:{}", publishDateMap);
        }
        // link
        Set<Long> set = new HashSet<>(customDemandIds);
        List<ProductCustomDemandDO> list = set.stream().map(i -> {
            ProductCustomDemandDO customDemandDO = new ProductCustomDemandDO();
            customDemandDO.setProductDemandId(productDemandId);
            customDemandDO.setCustomDemandId(i);
            return customDemandDO;
        }).collect(Collectors.toList());
        productCustomDemandMapper.batchInsert(list);
        // link after
        if(updatePublishDate){
            after(publishDateMap, customDemandIds);
        }
    }

    @Override
    public void batchInsert(List<Long> productDemandIds, Long customDemandId) {
        log.info("客户需求详情,新增关联关系:{},{}", productDemandIds, customDemandId);
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return;
        }
        ProductCustomDemandCondition c = ProductCustomDemandCondition.builder().customDemandId(customDemandId).isDeleted(false).build();
        List<ProductCustomDemandDO> exists = productCustomDemandMapper.select(c);
        List<Long> existProductDemandIds = exists.stream().map(ProductCustomDemandDO::getProductDemandId).collect(Collectors.toList());
        productDemandIds.removeAll(existProductDemandIds);
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return;
        }
        // link before
        Map<Long, Date> publishDateMap = new HashMap<>();
        before(publishDateMap, Lists.newArrayList(customDemandId));
        log.info("customDemandId,publishDate:{}", publishDateMap);
        // link
        Set<Long> set = new HashSet<>(productDemandIds);
        List<ProductCustomDemandDO> list = set.stream().map(i -> {
            ProductCustomDemandDO customDemandDO = new ProductCustomDemandDO();
            customDemandDO.setProductDemandId(i);
            customDemandDO.setCustomDemandId(customDemandId);
            return customDemandDO;
        }).collect(Collectors.toList());
        productCustomDemandMapper.batchInsert(list);
        // link after
        after(publishDateMap, Lists.newArrayList(customDemandId));
    }

    private void before(Map<Long, Date> publishDateMap, List<Long> customDemandIds) {
        if (!CollectionUtils.isEmpty(customDemandIds)) {
            customDemandIds.forEach(bid -> {
                Date publishDate = customDemandComponent.getProjectEndDate(bid);
                publishDateMap.put(bid, publishDate);
            });
        }
    }

    private void after(Map<Long, Date> publishDateMap, List<Long> customDemandIds) {
        List<BizChangeLogDO> logs = new ArrayList<>();
        customDemandIds.forEach(bid -> {
            customDemandComponent.updateProjectEndDate(bid);
            CustomDemandDO customDemandDO = customDemandMapper.selectById(bid);
            if (!Objects.equals(publishDateMap.get(bid), customDemandDO.getProjectEndDate())) {
                String oldValue = DateUtil.parseToString(publishDateMap.get(bid), DateStyle.YYYY_MM_DD);
                String newValue = DateUtil.parseToString(customDemandDO.getProjectEndDate(), DateStyle.YYYY_MM_DD);
                logs.add(customDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, bid));
            }
        });
        if (CollectionUtils.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }
}

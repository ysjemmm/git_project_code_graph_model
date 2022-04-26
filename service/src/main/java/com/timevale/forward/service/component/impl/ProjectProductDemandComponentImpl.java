package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
public class ProjectProductDemandComponentImpl implements ProjectProductDemandComponent {

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Resource
    private BizDemandLogComponent bizDemandLogComponent;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;


    @Override
    public void update(Long projectId, Long productDemandId) {
        log.info("删除项目与产品需求关系,projectId={},productDemandId={}", projectId, productDemandId);
        // unlink before
        List<Long> productDemandIds = new ArrayList<>();
        if (productDemandId != null) {
            //产品需求暂停,作废,删除与项目的关联
            productDemandIds.add(productDemandId);
        } else {
            //项目作废
            productDemandIds = projectProductDemandMapper.getByProjectId(projectId)
                    .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        }
        Map<Long, Date> publishDateMap = new HashMap<>();
        List<Long> bizDemandIds = new ArrayList<>();
        before(productDemandIds, publishDateMap, bizDemandIds);
        log.info("bizDemandId,publishDate:{}",publishDateMap);
        // unlink
        ProjectProductDemandDO projectProductDemandDO = new ProjectProductDemandDO();
        projectProductDemandDO.setProjectId(projectId);
        projectProductDemandDO.setProductDemandId(productDemandId);
        projectProductDemandDO.setIsDeleted(true);
        projectProductDemandMapper.update(projectProductDemandDO);
        // unlink after
        after(publishDateMap, bizDemandIds);
    }

    @Override
    public void batchInsert(Long projectId, List<Long> productDemandIds) {
        log.info("新增项目与产品需求关系,projectId={},productDemandId={}", projectId, productDemandIds);
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectId);
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        productDemandIds.removeAll(existProductDemandIds);
        // link before
        Map<Long, Date> publishDateMap = new HashMap<>();
        List<Long> bizDemandIds = new ArrayList<>();
        before(productDemandIds, publishDateMap, bizDemandIds);
        log.info("bizDemandId,publishDate:{}",publishDateMap);
        // link
        if (!CollectionUtils.isEmpty(productDemandIds)) {
            Set<Long> set = new HashSet<>(productDemandIds);
            List<ProjectProductDemandDO> list = set.stream().map(i -> {
                ProjectProductDemandDO productDemandDO = new ProjectProductDemandDO();
                productDemandDO.setProductDemandId(i);
                productDemandDO.setProjectId(projectId);
                return productDemandDO;
            }).collect(Collectors.toList());
            projectProductDemandMapper.batchInsert(list);
        }
        // link after
        after(publishDateMap, bizDemandIds);
    }


    private void before(List<Long> productDemandIds, Map<Long, Date> publishDateMap, List<Long> bizDemandIds) {
        if (!CollectionUtils.isEmpty(productDemandIds)) {
            List<Long>bids = productBizDemandMapper.getByProductDemandIds(productDemandIds)
                    .stream().map(ProductBizDemandDO::getBizDemandId).collect(Collectors.toList());
            bizDemandIds.addAll(bids);
            bizDemandIds.forEach(bid -> {
                Date publishDate = bizDemandComponent.getProjectEndDate(bid);
                publishDateMap.put(bid, publishDate);
            });
        }
    }

    private void after(Map<Long, Date> publishDateMap, List<Long> bizDemandIds) {
        List<BizChangeLogDO> logs = new ArrayList<>();
        bizDemandIds.stream().distinct().forEach(bid -> {
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

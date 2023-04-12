package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.service.component.*;
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

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private ProductCustomDemandMapper productCustomDemandMapper;

    @Resource
    private CustomDemandComponent customDemandComponent;

    @Resource
    private CustomDemandMapper customDemandMapper;

    @Resource
    private CustomDemandLogComponent customDemandLogComponent;

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

        Map<Long, Date> bizPublishDateMap = new HashMap<>();
        List<Long> bizDemandIds = new ArrayList<>();
        Map<Long, Date> customPublishDateMap = new HashMap<>();
        List<Long> customDemandIds = new ArrayList<>();

        before(productDemandIds, bizPublishDateMap, bizDemandIds,customPublishDateMap,customDemandIds);
        log.info("bizPublishDateMap,customPublishDateMap:{},{}", bizPublishDateMap,customPublishDateMap);
        // unlink
        ProjectProductDemandDO projectProductDemandDO = new ProjectProductDemandDO();
        projectProductDemandDO.setProjectId(projectId);
        projectProductDemandDO.setProductDemandId(productDemandId);
        projectProductDemandDO.setIsDeleted(true);
        projectProductDemandMapper.update(projectProductDemandDO);
        // unlink after
        after(bizPublishDateMap, bizDemandIds,customPublishDateMap,customDemandIds);
    }

    @Override
    public void batchInsert(Long projectId, List<Long> productDemandIds) {
        log.info("新增项目与产品需求关系,projectId={},productDemandId={}", projectId, productDemandIds);
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectId);
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        productDemandIds.removeAll(existProductDemandIds);
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return;
        }
        // link before
        Map<Long, Date> bizPublishDateMap = new HashMap<>();
        List<Long> bizDemandIds = new ArrayList<>();
        Map<Long, Date> customPublishDateMap = new HashMap<>();
        List<Long> customDemandIds = new ArrayList<>();

        before(productDemandIds, bizPublishDateMap, bizDemandIds,customPublishDateMap,customDemandIds);
        log.info("bizPublishDateMap,customPublishDateMap:{},{}", bizPublishDateMap,customPublishDateMap);
        // link
        Set<Long> set = new HashSet<>(productDemandIds);
        List<ProjectProductDemandDO> list = set.stream().map(i -> {
            ProjectProductDemandDO productDemandDO = new ProjectProductDemandDO();
            productDemandDO.setProductDemandId(i);
            productDemandDO.setProjectId(projectId);
            return productDemandDO;
        }).collect(Collectors.toList());
        projectProductDemandMapper.batchInsert(list);
        // link after
        after(bizPublishDateMap, bizDemandIds,customPublishDateMap,customDemandIds);
    }


    private void before(List<Long> productDemandIds, Map<Long, Date> publishDateMap, List<Long> bizDemandIds
            ,Map<Long, Date> customPublishDateMap, List<Long> customDemandIds) {
        if (!CollectionUtils.isEmpty(productDemandIds)) {
            List<Long> bids = productBizDemandMapper.selectByProductDemandIds(productDemandIds)
                    .stream().map(ProductBizDemandDO::getBizDemandId).distinct().collect(Collectors.toList());
            bizDemandIds.addAll(bids);
            bizDemandIds.forEach(bid -> {
                BizDemandDO bizDemandDO = bizDemandMapper.get(bid);
                publishDateMap.put(bid, bizDemandDO.getProjectEndDate());
            });


            List<Long> cIds = productCustomDemandMapper.selectByProductDemandIds(productDemandIds)
                    .stream().map(ProductCustomDemandDO::getCustomDemandId).distinct().collect(Collectors.toList());
            customDemandIds.addAll(cIds);
            customDemandIds.forEach(cid -> {
                CustomDemandDO customDemandDO = customDemandMapper.selectById(cid);
                customPublishDateMap.put(cid, customDemandDO.getProjectEndDate());
            });
        }
    }

    private void after(Map<Long, Date> publishDateMap, List<Long> bizDemandIds,Map<Long, Date> customPublishDateMap, List<Long> customDemandIds) {
        List<BizChangeLogDO> logs = new ArrayList<>();
        bizDemandIds.forEach(bid -> {
            bizDemandComponent.updateProjectEndDate(bid);
            BizDemandDO bizDemandDO = bizDemandMapper.get(bid);
            if (!Objects.equals(publishDateMap.get(bid), bizDemandDO.getProjectEndDate())) {
                String oldValue = DateUtil.parseToString(publishDateMap.get(bid), DateStyle.YYYY_MM_DD);
                String newValue = DateUtil.parseToString(bizDemandDO.getProjectEndDate(), DateStyle.YYYY_MM_DD);
                logs.add(bizDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, bid));
            }
        });

        customDemandIds.forEach(cid -> {
            customDemandComponent.updateProjectEndDate(cid);
            CustomDemandDO customDemandDO = customDemandMapper.selectById(cid);
            if (!Objects.equals(customPublishDateMap.get(cid), customDemandDO.getProjectEndDate())) {
                String oldValue = DateUtil.parseToString(customPublishDateMap.get(cid), DateStyle.YYYY_MM_DD);
                String newValue = DateUtil.parseToString(customDemandDO.getProjectEndDate(), DateStyle.YYYY_MM_DD);
                logs.add(customDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, cid));
            }
        });

        if (CollectionUtils.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }
}

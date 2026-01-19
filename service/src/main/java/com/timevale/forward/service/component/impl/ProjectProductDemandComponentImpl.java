package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.CustomDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductCustomDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.dal.entity.ProductCustomDemandDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.facade.api.request.ProjectProductDemandLinkReq;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.CustomDemandComponent;
import com.timevale.forward.service.component.CustomDemandLogComponent;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.component.ProductDemandLogComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.component.TaskProductDemandComponent;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProductDemandComponent productDemandComponent;

    @Resource
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Resource
    private ProjectLogComponent projectLogComponent;

    @Resource
    private ProductDemandLogComponent productDemandLogComponent;

    @Resource
    private TaskProductDemandComponent taskProductDemandComponent;

    @Override
    public void linkOrUnLinkProductDemand(ProjectProductDemandLinkReq productDemandLinkReq) {
        ProjectDO projectDO = projectMapper.get(productDemandLinkReq.getProjectId());
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        List<Long> productDemandIds = productDemandLinkReq.getProductDemandIds();
        List<ProductDemandDO> productDemands = productDemandMapper.selectByIdList(productDemandIds);
        Map<Long, String> pdNameMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getName, (v1, v2) -> v2));
        Map<Long, Integer> statusMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getStatus, (v1, v2) -> v2));

        if (LinkOrUnLinkEnum.LINK.getCode().equals(productDemandLinkReq.getType())) {
            List<ProjectProductDemandDO> productDemand = projectProductDemandMapper.getLinkedProductDemand(productDemandIds);
            if (CollectionUtils.isNotEmpty(productDemand)) {
                List<Long> existedIds = productDemand.stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
                throw new BaseBizRuntimeException("产品需求id为" + existedIds + "已被项目关联,请刷后重试");
            }
            List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByProductDemandIds(productDemandIds);
            Map<Long, Integer> bizIdMap = productBizDemandDOList.stream().collect(Collectors.toMap(ProductBizDemandDO::getBizDemandId, ProductBizDemandDO::getStatus, (v1, v2) -> v2));

            productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus(), productDemandIds);

            projectProductDemandComponent.batchInsert(projectDO.getId(), productDemandIds);

            //项目关联后,业务需求的发布时间可能变化
            bizIdMap.forEach((k, v) -> {
                BizDemandDO bizDemandDO = bizDemandMapper.get(k);
                productDemandComponent.sendDingMsg(v, bizDemandDO.getStatus(), k);
            });

            projectLogComponent.addLogWhenLinkOrUnlink(projectDO.getName(), projectDO.getId(), pdNameMap, ButtonActionEnum.LINK.getText());

        } else {
            // 取消关联时，需要检查状态是否可以回退
            // 状态只能前进，不能回退：如果当前状态已经高于WAITING，则不修改状态
            Integer newStatus = ProductDemandStatusEnum.WAITING.getCode();
            for (Long productDemandId : productDemandIds) {
                Integer currentStatus = statusMap.get(productDemandId);
                // 特殊状态（暂停/作废）或已经高于WAITING的状态，不回退
                if (currentStatus != null && (currentStatus < 0 || currentStatus > newStatus)) {
                    log.info("需求{}当前状态{}，取消关联时不回退状态", productDemandId, currentStatus);
                } else {
                    ProductDemandDO productDemandDO = new ProductDemandDO();
                    productDemandDO.setId(productDemandId);
                    productDemandDO.setStatus(newStatus);
                    productDemandComponent.update(productDemandDO);
                }

                projectProductDemandComponent.update(null, productDemandId);
            }

            // 一个产品需求下的业务需求
            productDemandComponent.updateDemandStatusAsProductStatusChange(productDemandIds, false);

            projectLogComponent.addLogWhenLinkOrUnlink(projectDO.getName(), projectDO.getId(), pdNameMap, ButtonActionEnum.UN_LINK.getText());
            
            // 只记录实际被修改的需求的日志
            Map<Long, Integer> actualUpdatedStatusMap = statusMap.entrySet().stream()
                .filter(e -> e.getValue() == null || (e.getValue() >= 0 && e.getValue() <= newStatus))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
            if (!actualUpdatedStatusMap.isEmpty()) {
                productDemandLogComponent.addLogAsProjectStatusChange(actualUpdatedStatusMap, newStatus);
            }

            // 取消产品需求和任务的关联
            productDemandIds.forEach(a -> taskProductDemandComponent.update(null, a));
        }
    }

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

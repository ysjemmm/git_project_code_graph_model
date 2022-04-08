package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.facade.api.client.DataCorrectService;
import com.timevale.forward.facade.api.request.DataModifyReq;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.DataCorrectTypeEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class DataCorrectServiceImpl implements DataCorrectService {

    @Resource
    private ProjectComponent projectComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProductDemandComponent productDemandComponent;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;


    @Override
    public BaseResult<Boolean> modify(DataModifyReq dataModifyReq) {
        if (DataCorrectTypeEnum.PROJECT.getCode().equals(dataModifyReq.getType())) {
            List<ProjectDO> projectDOList = projectMapper.getByIds(dataModifyReq.getIds());
            projectDOList.forEach(a -> {
                if (!ProjectStatusEnum.SUSPEND.getCode().equals(a.getStatus())) {
                    List<ProjectNodeDO> projectNodes = projectNodeMapper.get(a.getId());
                    projectComponent.fillInfo(projectNodes, a);
                    a.setRetainModifyDate(true);
                    projectMapper.update(a);
                    updateProductDemandStatus(a.getId(), a.getStatus());
                }
            });
            log.info("数据订正,更新项目完成");
        } else if (DataCorrectTypeEnum.PRODUCT_DEMAND.getCode().equals(dataModifyReq.getType())) {
            List<ProjectDO> projectDOList = projectMapper.selectByProductDemandIdList(dataModifyReq.getIds());
            projectDOList.forEach(a -> {
                updateProductDemandStatus(a.getId(), a.getStatus());
            });
            log.info("数据订正,更新产品需求完成");
        } else if (DataCorrectTypeEnum.BIZ_DEMAND.getCode().equals(dataModifyReq.getType())) {
            List<Long> bizDemandIds = dataModifyReq.getIds();
            Map<Integer, List<Long>> condition = new HashMap<>();
            bizDemandIds.forEach(a->{
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(a);
                productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o))
                        .ifPresent(minStauts -> productDemandComponent.processBizDemandStatus(condition, minStauts, a));
            });
            condition.forEach((k, v) -> {
                //更新产品需求下的所有业务需求状态
                bizDemandMapper.updateByIds(v, k,true);
            });
            log.info("数据订正,更新业务需求完成");
        }
        return BaseResult.success();
    }

    @Override
    public BaseResult<Boolean> calculateStatus() {
        List<Integer> status = Lists.newArrayList(ProjectStatusEnum.DEVING.getCode(), ProjectStatusEnum.TESTING.getCode());
        List<ProjectDO> list = projectMapper.getByStatus(status);
        list.forEach(a->{
            List<ProjectNodeDO> projectNodes = projectNodeMapper.get(a.getId());
            projectComponent.fillInfo(projectNodes, a);
            a.setRetainModifyDate(true);
            projectMapper.update(a);
            updateProductDemandStatus(a.getId(), a.getStatus());
        });
        log.info("数据订正,状态变更完成");
        return BaseResult.success();
    }

    private void updateProductDemandStatus(Long projectId, Integer status) {
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectId);
        if (CollectionUtils.isEmpty(exists)) {
            log.info("更新产品需求,没有找到产品需求");
            return;
        }
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        if (ProjectStatusEnum.WAITING.getCode().equals(status)
                || ProjectStatusEnum.SUSPEND.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.INCLUDED.getCode(),true);
        } else if (ProjectStatusEnum.PLANING.getCode().equals(status)
                || ProjectStatusEnum.DEVING.getCode().equals(status)
                || ProjectStatusEnum.TESTING.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.PROGRESS.getCode(),true);
        } else if (ProjectStatusEnum.RELEASED.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.ONLINE.getCode(),true);
        }
        updateBizDemandStatusAsProductStatusChange(existProductDemandIds, false);
    }

    public void updateBizDemandStatusAsProductStatusChange(List<Long> productDemandIds, boolean bizProductDemandUnLink) {
        if (CollectionUtils.isEmpty(productDemandIds)) {
            log.info("产品需求变化-更新业务需求,产品需求id不存在");
            return;
        }
        if (bizProductDemandUnLink) {
            //解除产品需求和业务需求关系(含作废情况)
            productDemandIds.forEach(p -> {
                buildConditionBeforeUpdate(Lists.newArrayList(p), bizProductDemandUnLink);
            });
        } else {
            buildConditionBeforeUpdate(productDemandIds, bizProductDemandUnLink);
        }
    }


    private void buildConditionBeforeUpdate(List<Long> productDemandIds, boolean bizProductDemandUnLink) {
        log.info("产品需求变化-更新业务需求,产品需求id={},解除二者关联={}", productDemandIds, bizProductDemandUnLink);
        // 产品需求下的所有业务需求
        List<ProductBizDemandDO> bizDemands = productBizDemandMapper.getByProductDemandIds(productDemandIds);
        if (CollectionUtils.isEmpty(bizDemands)) {
            log.info("产品需求变化-更新业务需求,业务需求不存在");
            return;
        }
        // 业务需求id去重
        Map<Long, ProductBizDemandDO> bizDemandMap = bizDemands.stream()
                .collect(Collectors.toMap(ProductBizDemandDO::getBizDemandId, k -> k, (v1, v2) -> v2));
        Map<Integer, List<Long>> condition = new HashMap<>();
        bizDemandMap.forEach((k, v) -> {
            //被驳回和作废的业务需求不处理
            if (!BizDemandStatusEnum.REJECT.getCode().equals(v.getStatus()) && !BizDemandStatusEnum.INVALID.getCode().equals(v.getStatus())) {
                //当前业务需求下的所有产品需求
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(k);
                if (bizProductDemandUnLink) {
                    //如果是解除关联:计算业务需求状态时需要过滤掉本次被解除的产品需求
                    Integer minStatus = productDemands.stream().filter(i -> !productDemandIds.get(0).equals(i.getProductDemandId())).map(ProductBizDemandDO::getStatus)
                            .min(Comparator.comparingInt(o -> o)).orElse(null);
                    if (minStatus == null) {
                        //业务需求只关联一个产品需求后且被解除
                        condition.computeIfAbsent(BizDemandStatusEnum.RECEIVED.getCode(), value -> new ArrayList<>()).add(k);
                    } else {
                        productDemandComponent.processBizDemandStatus(condition, minStatus, k);
                    }
                } else {
                    Integer minStauts = productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);
                    if (minStauts != null) {
                        productDemandComponent.processBizDemandStatus(condition, minStauts, k);
                    }
                }
            }
        });
        condition.forEach((k, v) -> {
            //更新产品需求下的所有业务需求状态
            bizDemandMapper.updateByIds(v, k,true);
        });
        log.info("产品需求变化-更新业务需求:产品需求id={},需要更新的业务需求状态和id={}", productDemandIds, condition);
    }
}

package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.DataCorrectService;
import com.timevale.forward.facade.api.request.DataModifyReq;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;
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
    private BizDemandComponent bizDemandComponent;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProjectNodeComponent projectNodeComponent;

    @Resource
    private TestBillMapper testBillMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(DataModifyReq dataModifyReq) {
        if (DataCorrectTypeEnum.PROJECT.getCode().equals(dataModifyReq.getType())) {
            List<ProjectDO> projectDOList = projectMapper.getByIds(dataModifyReq.getIds());
            projectDOList.forEach(a -> {
                if (!ProjectStatusEnum.SUSPEND.getCode().equals(a.getStatus())) {
                    List<ProjectNodeDO> projectNodes = projectNodeMapper.get(a.getId());
                    projectComponent.fillInfo(projectNodes, a);
                    projectMapper.updateStatus(a);
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
            bizDemandIds.forEach(a -> {
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(a);
                productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o))
                        .ifPresent(minStauts -> productDemandComponent.processBizDemandStatus(condition, minStauts, a));
            });
            condition.forEach((k, v) -> {
                //更新产品需求下的所有业务需求状态
                bizDemandMapper.updateByIds(v, k, true);
            });
            log.info("数据订正,更新业务需求完成");
        }
        return BaseResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> calculateStatus() {
        List<Integer> status = Lists.newArrayList(ProjectStatusEnum.PLANING.getCode(), ProjectStatusEnum.DEVING.getCode(), ProjectStatusEnum.TESTING.getCode());
        List<ProjectDO> list = projectMapper.getByStatus(status);
        list.forEach(a -> {
            List<ProjectNodeDO> projectNodes = projectNodeMapper.get(a.getId());
            projectComponent.fillInfo(projectNodes, a);
            projectMapper.updateStatus(a);
            updateProductDemandStatus(a.getId(), a.getStatus());
        });
        log.info("数据订正,状态变更完成");
        return BaseResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> nodeStatusUpdate() {
        List<Long> projectIdList = projectMapper.getAllId();

        projectIdList.forEach(e -> {
            // 查询项目节点
            List<ProjectNodeDO> nodeDOList = projectNodeComponent.get(e);

            // 如果节点为空则状态设为待启动
            Integer nodeStatus;
            if (org.apache.commons.collections.CollectionUtils.isEmpty(nodeDOList)) {
                nodeStatus = ProjectNodeStatusEnum.READY_START.getCode();
            } else {
                nodeStatus = ProjectNodeStatusEnum.getStatus(nodeDOList);
            }
            // 更新项目节点状态
            ProjectDO projectDO = new ProjectDO();
            projectDO.setId(e);
            projectDO.setNodeStatus(nodeStatus);
            projectMapper.updateNodeStatus(projectDO);
        });

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> bizDemandProjectEndDateUpdate() {
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.selectAll();
        bizDemandDOList = bizDemandDOList.stream().filter(e -> !e.getIsDeleted()).collect(Collectors.toList());

        for (BizDemandDO e : bizDemandDOList) {
            Date projectEndDate = bizDemandComponent.getProjectEndDate(e.getId());
            if (projectEndDate != null) {
                bizDemandMapper.updateDate(e.getId(), projectEndDate, DateUtil.getMonth(projectEndDate) - 1);
            }
        }

        return BaseResult.success(true);
    }


    @Override
    public BaseResult<Boolean> updateDelayDays() {
        List<ProjectNodeDO> projectNodeDos = projectNodeMapper.listByName(ProjectNodeEnum.SUBMIT_TEST.getText())
                .stream().filter(a -> a.getActualDate() != null && a.getPlanDate() != null).collect(Collectors.toList());
        projectNodeDos.forEach(a -> {
            TestBillDO testBillDO = new TestBillDO();

            String planDate = DateUtil.parseToString(a.getPlanDate(), DateFormatConst.DATE_FORMAT);
            String actualDate = DateUtil.parseToString(a.getActualDate(), DateFormatConst.DATE_FORMAT);

            testBillDO.setDelayDay(DateUtil.getIntervalDays(planDate, actualDate));

            testBillDO.setProjectId(a.getProjectId());
            testBillMapper.updateDelayDay(testBillDO);
        });
        log.info("数据订正,逾期时间更新完成");
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateBizDemandStatus() {
        List<Integer> status = Lists.newArrayList(BizDemandStatusEnum.RECEIVED.getCode());
        List<BizDemandDO> bizDemandDos = bizDemandMapper.selectByStatus(status);
        Map<Integer, List<Long>> condition = new HashMap<>();
        bizDemandDos.forEach(a -> {
            //被驳回和作废的业务需求不处理
            if (!BizDemandStatusEnum.REJECT.getCode().equals(a.getStatus()) && !BizDemandStatusEnum.INVALID.getCode().equals(a.getStatus())) {
                //当前业务需求下的所有产品需求
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(a.getId());
                Integer minStauts = productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);
                if (minStauts != null) {
                    productDemandComponent.processBizDemandStatus(condition, minStauts, a.getId());
                }
            }
        });
        condition.forEach((k, v) -> {
            //更新产品需求下的所有业务需求状态
            bizDemandMapper.updateByIds(v, k, true);
        });
        log.info("数据订正,业务需求状态变更完成,更新的数据 :{}", condition);
        return BaseResult.success(true);
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
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.INCLUDED.getCode(), true);
        } else if (ProjectStatusEnum.PLANING.getCode().equals(status)
                || ProjectStatusEnum.DEVING.getCode().equals(status)
                || ProjectStatusEnum.TESTING.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.PROGRESS.getCode(), true);
        } else if (ProjectStatusEnum.RELEASED.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.ONLINE.getCode(), true);
        }
        updateBizDemandStatusAsProductStatusChange(existProductDemandIds);
    }

    private void updateBizDemandStatusAsProductStatusChange(List<Long> productDemandIds) {
        if (CollectionUtils.isEmpty(productDemandIds)) {
            log.info("产品需求变化-更新业务需求,产品需求id不存在");
            return;
        }
        //解除产品需求和业务需求关系(含作废情况)
        buildConditionBeforeUpdate(productDemandIds);

    }

    private void buildConditionBeforeUpdate(List<Long> productDemandIds) {
        log.info("产品需求变化-更新业务需求,产品需求id={}", productDemandIds);
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
                Integer minStauts = productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);
                if (minStauts != null) {
                    productDemandComponent.processBizDemandStatus(condition, minStauts, k);
                }
            }
        });
        condition.forEach((k, v) -> {
            //更新产品需求下的所有业务需求状态
            bizDemandMapper.updateByIds(v, k, true);
        });
        log.info("产品需求变化-更新业务需求:产品需求id={},需要更新的业务需求状态和id={}", productDemandIds, condition);
    }


}

package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ProductDemandGroupQueryCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.CustomDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductCustomDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandDescFlowMapper;
import com.timevale.forward.dal.dao.ProductDemandDescRecordMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.ForwardFlowStatusEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProductDemandTypeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.BizPermissionScopeEnum;
import com.timevale.forward.service.constant.BizPermissionTypeEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.observer.event.BizDemandStatusAloneChangeMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandStatusChangeMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.richtext.RichTextImageUrlRefresher;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProductDemandComponentImpl implements ProductDemandComponent {

    @Resource
    private ProductDemandMapper productDemandMapper;
    @Resource
    private FileComponent fileComponent;
    @Resource
    private PersonComponent personComponent;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private BizDomainMapper bizDomainMapper;
    @Resource
    private ProductBizDemandMapper productBizDemandMapper;
    @Resource
    private ProductCustomDemandMapper productCustomDemandMapper;
    @Resource
    private BizDemandMapper bizDemandMapper;
    @Resource
    private CustomDemandMapper customDemandMapper;
    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;
    @Resource
    private BizDemandComponent bizDemandComponent;
    @Resource
    private MessageEventPublisher messageEventPublisher;
    @Resource
    private BizDemandLogComponent bizDemandLogComponent;
    @Resource
    private ProductDemandLogComponent productDemandLogComponent;
    @Resource
    private ProjectLogComponent projectLogComponent;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProductDemandDescRecordMapper productDemandDescRecordMapper;
    @Resource
    private ProductDemandDescFlowMapper productDemandDescFlowMapper;
    @Resource
    private BizPermissionOwnerComponent bizPermissionOwnerComponent;
    @Resource
    private RichTextImageUrlRefresher richTextImageUrlRefresher;

    @Override
    public List<ProductDemandListDO> list(ProductDemandListCondition condition) {
        condition.setName(StringUtil.toLikeStr(condition.getName()));
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        return productDemandMapper.list(condition);
    }
    @Override
    public ProductDemandDetailVO get(Long id) {
        ProductDemandDO demandDO = productDemandMapper.get(id);
        if (demandDO == null) {
            throw new BaseBizRuntimeException("该产品需求不存在");
        }
        ProductDemandDetailVO demandDetailVO = ProductDemandCopier.INSTANCE.convert(demandDO);
        demandDetailVO.setDesc(richTextImageUrlRefresher.refresh(demandDetailVO.getDesc()));
        demandDetailVO.setStatusName(ProductDemandStatusEnum.getTextByCode(demandDetailVO.getStatus()));
        demandDetailVO.setPriorityName(PriorityEnum.getTextByCode(demandDetailVO.getPriority()));
        List<Integer> list = JSON.parseArray(demandDO.getType(), Integer.class);
        demandDetailVO.setTypes(list);
        demandDetailVO.setTypeName(ProductDemandTypeEnum.getTextByCode(list));

        //产品线
        ProductLineDO productLineDO = productLineMapper.selectById(demandDO.getProductLineId());

        BizDomainDO bizDomainDO = bizDomainMapper.selectById(productLineDO.getBizDomainId());
        ProductLineVO productLineVO = ProductLineCopier.INSTANCE.convert(productLineDO);
        productLineVO.setBizDomainOwnerId(bizDomainDO.getOwnerId());
        productLineVO.setBizDomainName(bizDomainDO.getName());
        productLineVO.setBizDomainOwner(bizDomainDO.getOwner());
        demandDetailVO.setProductLineVO(productLineVO);
        // 补充产品线【其他负责人】信息
        List<BizPermissionOwnerDO> productDemandOwnerList = bizPermissionOwnerComponent.getProductDemandOwnerList(BizPermissionTypeEnum.PRODUCT_DEMAND_MODIFY,
                BizPermissionScopeEnum.PRODUCT_LINE_SCOPE, productLineDO.getId());
        if (CollUtil.isNotEmpty(productDemandOwnerList)) {
            List<PersonVO> otherOwners = productDemandOwnerList.stream().map(e -> new PersonVO().setUserId(e.getOwnerId()).setUserName(e.getOwner())).collect(Collectors.toList());
            productLineVO.setOtherOwners(otherOwners);
        }

        //附件
        List<FileDO> fileDO = fileComponent.select(id, FileTypeEnum.PRODUCT_DEMAND.getCode());
        demandDetailVO.setFiles(FileCopier.INSTANCE.transform(fileDO));

        // 抄送人
        List<PersonDO> personDO = personComponent.select(id, PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
        demandDetailVO.setRecipients(PersonCopier.INSTANCE.transform(personDO));

        // 变更次数
        Integer changeTimes = productDemandDescRecordMapper.countByProductDemandId(demandDO.getId());
        demandDetailVO.setDescChangeTimes(changeTimes == 0 ? changeTimes : changeTimes - 1);

        // 变更后描述
        ProductDemandDescFlowDO latestDescFlow =
                productDemandDescFlowMapper.getLastByProductDemandId(demandDO.getId());
        if (latestDescFlow != null && ForwardFlowStatusEnum.AUDITING.getCode().equals(latestDescFlow.getStatus())) {
            demandDetailVO.setChangeDesc(richTextImageUrlRefresher.refresh(latestDescFlow.getChangeDesc()));
        }

        // 资源评估信息
        demandDetailVO.setResourcePlans(ProductDemandCopier.INSTANCE.convert(productDemandMapper.listProductDemandOwners(Collections.singletonList(id)), true));
        return demandDetailVO;
    }

    @Override
    public void update(ProductDemandDO productDemandDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        productDemandDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        productDemandDO.setModifyManId(userInfo.getId());
        productDemandMapper.update(productDemandDO);
    }

    @Override
    public void updateProductDemandStatus(Long projectId, Integer status) {
        updateProductDemandStatus(projectId, status, new ArrayList<>());
    }

    @Override
    public void updateProductDemandStatus(Long projectId, Integer status, List<Long> productDemandIds) {
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectId);
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        
        // 计算目标状态
        Integer targetStatus = calculateTargetStatus(status);
        
        // 收集所有需要检查的需求ID
        List<Long> allDemandIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(productDemandIds)) {
            allDemandIds.addAll(productDemandIds);
        }
        if (!CollectionUtils.isEmpty(existProductDemandIds)) {
            // 去重添加
            for (Long id : existProductDemandIds) {
                if (!allDemandIds.contains(id)) {
                    allDemandIds.add(id);
                }
            }
        }
        
        if (CollectionUtils.isEmpty(allDemandIds)) {
            log.info("更新产品需求,没有找到产品需求");
            return;
        }
        
        // 查询所有需求的当前状态
        List<ProductDemandDO> allDemands = productDemandMapper.selectByIdList(allDemandIds);
        
        // 筛选出可以更新的需求（状态只能前进，不能回退）
        List<Long> needUpdateIds = new ArrayList<>();
        for (ProductDemandDO demand : allDemands) {
            if (canUpdateStatus(demand.getStatus(), targetStatus)) {
                needUpdateIds.add(demand.getId());
            } else {
                log.info("需求{}当前状态{}已经高于目标状态{}，跳过更新", 
                    demand.getId(), demand.getStatus(), targetStatus);
            }
        }
        
        if (CollectionUtils.isEmpty(needUpdateIds)) {
            log.info("更新产品需求,没有需要更新的产品需求（所有需求状态都已经高于目标状态）");
            return;
        }

        List<ProductDemandDO> productDemands = productDemandMapper.selectByIdList(needUpdateIds);

        Map<Long, Integer> statusMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getStatus, (v1, v2) -> v2));
        Map<Long, String> nameMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getName, (v1, v2) -> v2));

        // 更新需求状态
        // 如果目标状态是"完成上线"(30)，需要同时更新上线时间
        if (ProductDemandStatusEnum.ONLINE.getCode().equals(targetStatus)) {
            // 获取项目的实际结束时间
            ProjectDO project = projectMapper.get(projectId);
            Date actualEndDate = project.getActualEndDate();
            
            if (actualEndDate != null) {
                // 只更新上线时间为空的产品需求
                int updatedCount = productDemandMapper.updateStatusAndOnlineTime(needUpdateIds, targetStatus, actualEndDate, true, false);
                log.info("项目{}状态变更为{}，自动更新{}个产品需求状态为完成上线，并设置上线时间为项目实际结束时间：{}", 
                    projectId, status, updatedCount, actualEndDate);
            } else {
                // 如果项目没有实际结束时间，只更新状态
                productDemandMapper.updateByIds(needUpdateIds, targetStatus, false);
                log.warn("项目{}状态变更为{}，但项目实际结束时间为空，只更新产品需求状态", projectId, status);
            }
        } else {
            // 其他状态只更新状态
            productDemandMapper.updateByIds(needUpdateIds, targetStatus, false);
        }
        
        if (ProjectStatusEnum.INVALID.getCode().equals(status)) {
            // unlink log
            String projectName = projectMapper.get(projectId).getName();
            projectLogComponent.addLogWhenLinkOrUnlink(projectName, projectId, nameMap, null);
        }

        productDemandLogComponent.addLogAsProjectStatusChange(statusMap, targetStatus);
        updateDemandStatusAsProductStatusChange(needUpdateIds, false);
    }
    
    /**
     * 判断是否可以更新状态（状态只能前进，不能回退）
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return true: 可以更新; false: 不能更新（会导致状态回退）
     */
    private boolean canUpdateStatus(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return true;
        }
        // 特殊状态处理：已暂停(-10)和已作废(-20)的需求不应该被自动更新
        if (currentStatus < 0) {
            log.info("需求当前状态为特殊状态{}，不自动更新", currentStatus);
            return false;
        }
        // 状态只能前进，不能回退（严格大于，相同状态不重复更新）
        return targetStatus > currentStatus;
    }
    
    /**
     * 根据项目状态计算产品需求的目标状态
     */
    private Integer calculateTargetStatus(Integer projectStatus) {
        if (ProjectStatusEnum.WAITING.getCode().equals(projectStatus)) {
            return ProductDemandStatusEnum.INCLUDED.getCode();
        } else if (ProjectStatusEnum.SUSPEND.getCode().equals(projectStatus)) {
            return ProductDemandStatusEnum.PJ_SUSPEND.getCode();
        } else if (ProjectStatusEnum.PLANING.getCode().equals(projectStatus)
                || ProjectStatusEnum.DEVING.getCode().equals(projectStatus)
                || ProjectStatusEnum.TESTING.getCode().equals(projectStatus)) {
            return ProductDemandStatusEnum.PROGRESS.getCode();
        } else if (ProjectStatusEnum.RELEASED.getCode().equals(projectStatus)
                || ProjectStatusEnum.CONCLUSION.getCode().equals(projectStatus)) {
            return ProductDemandStatusEnum.ONLINE.getCode();
        } else {
            return ProductDemandStatusEnum.WAITING.getCode();
        }
    }

    @Override
    public void updateDemandStatusAsProductStatusChange(List<Long> productDemandIds, boolean invalid) {
        //invalid为true时:作废,解除产品需求和业务需求关系
        updateBizDemandStatus(productDemandIds, invalid);

        updateCustomDemandStatus(productDemandIds, invalid);
    }

    @Override
    public void updateBizDemandStatus(List<Long> productDemandIds, boolean invalid) {
        log.info("产品需求变化-更新业务需求:{},{}", productDemandIds, invalid);
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return;
        }
        List<ProductBizDemandDO> bizDemands = productBizDemandMapper.getByProductDemandIds(productDemandIds);
        if (CollectionUtils.isEmpty(bizDemands)) {
            log.info("产品需求变化-更新业务需求,业务需求不存在");
            return;
        }
        // 业务需求id去重
        Map<Long, ProductBizDemandDO> bizDemandMap = bizDemands.stream()
                .collect(Collectors.toMap(ProductBizDemandDO::getBizDemandId, k -> k, (v1, v2) -> v2));
        bizDemandMap.forEach((bid, v) -> {
            //被驳回和作废的业务需求不处理
            if (!BizDemandStatusEnum.REJECT.getCode().equals(v.getStatus()) && !BizDemandStatusEnum.INVALID.getCode().equals(v.getStatus())) {
                //当前业务需求下的所有产品需求
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBdId(bid);
                Integer productStatus;
                if (invalid) {
                    //如果作废:计算业务需求状态时需要过滤掉本次被解除的产品需求
                    productStatus = productDemands.stream().filter(i -> !productDemandIds.get(0).equals(i.getProductDemandId())).map(ProductBizDemandDO::getStatus)
                            .min(Comparator.comparingInt(o -> o)).orElse(null);
                } else {
                    productStatus = productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);

                }
                Integer newStatus = updateDemandStatus(productStatus, bid, true);
                bizDemandLogComponent.addLogAsProductDemandStatusChange(v.getStatus(), newStatus, bid, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
                sendDingMsg(v.getStatus(), newStatus, bid);
            }
        });
    }

    @Override
    public void updateCustomDemandStatus(List<Long> productDemandIds, boolean invalid) {
        log.info("产品需求变化-更新客户需求:{},{}", productDemandIds, invalid);
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return;
        }
        List<ProductCustomDemandDO> customDemands = productCustomDemandMapper.getByProductDemandIds(productDemandIds);
        if (CollectionUtils.isEmpty(customDemands)) {
            log.info("产品需求变化-更新客户需求,客户需求不存在");
            return;
        }
        // 业务需求id去重
        Map<Long, ProductCustomDemandDO> customDemandMap = customDemands.stream()
                .collect(Collectors.toMap(ProductCustomDemandDO::getCustomDemandId, k -> k, (v1, v2) -> v2));
        customDemandMap.forEach((cid, v) -> {
            //被驳回客户需求不处理
            if (!BizDemandStatusEnum.REJECT.getCode().equals(v.getStatus())) {
                //当前业务需求下的所有产品需求
                List<ProductCustomDemandDO> productDemands = productCustomDemandMapper.getByCustomDemandId(cid);
                Integer productStatus;
                if (invalid) {
                    //如果作废:计算业务需求状态时需要过滤掉本次被解除的产品需求
                    productStatus = productDemands.stream().filter(i -> !productDemandIds.get(0).equals(i.getProductDemandId())).map(ProductCustomDemandDO::getStatus)
                            .min(Comparator.comparingInt(o -> o)).orElse(null);
                } else {
                    productStatus = productDemands.stream().map(ProductCustomDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);

                }
                Integer newStatus = updateDemandStatus(productStatus, cid, false);
                bizDemandLogComponent.addLogAsProductDemandStatusChange(v.getStatus(), newStatus, cid, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());
            }
        });
    }

    @Override
    public List<Long> getLinkBizDemandIds(List<Long> productDemandIds) {
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return Lists.emptyList();
        }
        List<Long> bizDemandIds = productBizDemandMapper.selectByProductDemandIds(productDemandIds)
                .stream().map(ProductBizDemandDO::getBizDemandId).collect(Collectors.toList());
        log.info("产品需求:{},关联的有业务需求:{}", productDemandIds, bizDemandIds);
        return bizDemandIds;
    }

    @Override
    public List<Long> getLinkCustomDemandIds(List<Long> productDemandIds) {
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return Lists.emptyList();
        }
        List<Long> customDemandIds = productCustomDemandMapper.selectByProductDemandIds(productDemandIds)
                .stream().map(ProductCustomDemandDO::getCustomDemandId).collect(Collectors.toList());
        log.info("产品需求:{},关联的客户需求:{}", productDemandIds, customDemandIds);
        return customDemandIds;
    }

    @Override
    public void sendDingMsg(Integer oldStatus, Integer newStatus, Long bizDemandId) {
        if (!Objects.equals(oldStatus, newStatus) && BizDemandStatusEnum.statusNeedNotice(newStatus)) {
            BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
            Date projectEndDate = bizDemandDO.getProjectEndDate();
            log.info("发送钉钉消息,项目发布时间={},更新前状态={},更新后状态={},业务需求id={}", projectEndDate, oldStatus, newStatus, bizDemandId);
            if (projectEndDate != null) {
                messageEventPublisher.publish(new BizDemandStatusChangeMsgEvent(
                        this,
                        bizDemandId,
                        bizDemandDO.getSubmitManId(),
                        bizDemandDO.getName(),
                        BizDemandStatusEnum.getTextByCode(newStatus),
                        projectEndDate)
                );
            } else {
                log.info("业务需求没有关联项目，发送钉钉消息");
                messageEventPublisher.publish(new BizDemandStatusAloneChangeMsgEvent(
                        this,
                        bizDemandId,
                        bizDemandDO.getSubmitManId(),
                        bizDemandDO.getName(),
                        BizDemandStatusEnum.getTextByCode(newStatus)
                ));
            }
        }
    }

    @Override
    public List<ProductDemandGroupFieldDO> getGroupTree(ProductDemandGroupQueryCondition condition) {
        condition.getCondition().setName(StringUtil.toLikeStr(condition.getCondition().getName()));
        condition.getCondition().setCreateDateStart(DateUtil.getStartOfDay(condition.getCondition().getCreateDateStart()));
        condition.getCondition().setCreateDateEnd(DateUtil.getEndOfDay(condition.getCondition().getCreateDateEnd()));
        return productDemandMapper.getGroupTree(condition);
    }

    @Override
    public List<ProductDemandListDO> getGroupList(ProductDemandGroupQueryCondition condition) {
        condition.getCondition().setName(StringUtil.toLikeStr(condition.getCondition().getName()));
        condition.getCondition().setCreateDateStart(DateUtil.getStartOfDay(condition.getCondition().getCreateDateStart()));
        condition.getCondition().setCreateDateEnd(DateUtil.getEndOfDay(condition.getCondition().getCreateDateEnd()));
        return productDemandMapper.getGroupList(condition);
    }

    @Override
    public List<ProductDemandGroupFieldDO> getSimpleGroupList(ProductDemandGroupQueryCondition condition) {
        condition.getCondition().setName(StringUtil.toLikeStr(condition.getCondition().getName()));
        condition.getCondition().setCreateDateStart(DateUtil.getStartOfDay(condition.getCondition().getCreateDateStart()));
        condition.getCondition().setCreateDateEnd(DateUtil.getEndOfDay(condition.getCondition().getCreateDateEnd()));
        return productDemandMapper.getSimpleGroupList(condition);
    }

    @Override
    public Long getSimpleGroupCount(ProductDemandGroupQueryCondition condition) {
        condition.getCondition().setName(StringUtil.toLikeStr(condition.getCondition().getName()));
        condition.getCondition().setCreateDateStart(DateUtil.getStartOfDay(condition.getCondition().getCreateDateStart()));
        condition.getCondition().setCreateDateEnd(DateUtil.getEndOfDay(condition.getCondition().getCreateDateEnd()));
        return productDemandMapper.getSimpleGroupCount(condition);
    }

    @Override
    public void updateDemandStatusWhenUnlink(Long demandId, Long productDemandId, boolean bizDemand) {
        if (bizDemand) {
            List<ProductBizDemandDO> demandDOList = productBizDemandMapper.getByBdId(demandId);

            Integer productStatus = demandDOList.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);

            Integer newStatus = bizDemandComponent.getBizDemandStatus(productStatus);
            BizDemandDO bizDemandDO = bizDemandMapper.get(demandId);
            Integer oldStatus = bizDemandDO.getStatus();
            log.info("产品需求删除关联,更新前状态={},更新后状态={},产品需求id={},业务需求id={}", oldStatus, newStatus, productDemandId, demandId);
            if (!Objects.equals(newStatus, oldStatus) && !BizDemandStatusEnum.statusNoNeedTodo(oldStatus)) {
                bizDemandDO.setStatus(newStatus);
                bizDemandMapper.update(bizDemandDO);
                bizDemandLogComponent.addLogAsProductDemandStatusChange(oldStatus, newStatus, demandId, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());

                if (BizDemandStatusEnum.statusNeedNotice(newStatus)) {
                    Date projectEndDate = bizDemandDO.getProjectEndDate();
                    messageEventPublisher.publish(new BizDemandStatusChangeMsgEvent(
                            this,
                            demandId,
                            bizDemandDO.getSubmitManId(),
                            bizDemandDO.getName(),
                            BizDemandStatusEnum.getTextByCode(newStatus),
                            projectEndDate)
                    );
                }
            }
        } else {
            List<ProductCustomDemandDO> demandDOList = productCustomDemandMapper.getByCustomDemandId(demandId);
            Integer productStatus = demandDOList.stream().map(ProductCustomDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);

            Integer newStatus = bizDemandComponent.getBizDemandStatus(productStatus);
            CustomDemandDO customDemandDO = customDemandMapper.selectById(demandId);
            Integer oldStatus = customDemandDO.getStatus();
            log.info("产品需求删除关联,更新前状态={},更新后状态={},产品需求id={},客户需求id={}", oldStatus, newStatus, productDemandId, demandId);
            if (!Objects.equals(newStatus, oldStatus) && !BizDemandStatusEnum.statusNoNeedTodo(oldStatus)) {
                customDemandDO.setStatus(newStatus);
                customDemandMapper.update(customDemandDO);
                bizDemandLogComponent.addLogAsProductDemandStatusChange(oldStatus, newStatus, demandId, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());
            }

        }

    }

    @Override
    public Integer updateDemandStatus(Integer productStatus, Long demandId, boolean bizDemand) {
        Integer status = bizDemandComponent.getBizDemandStatus(productStatus);
        if (bizDemand) {
            BizDemandDO bizDemandDO = new BizDemandDO();
            bizDemandDO.setId(demandId);
            bizDemandDO.setStatus(status);
            bizDemandMapper.update(bizDemandDO);
        } else {
            CustomDemandDO customDemandDO = new CustomDemandDO();
            customDemandDO.setId(demandId);
            customDemandDO.setStatus(status);
            customDemandMapper.update(customDemandDO);
        }
        return status;
    }

}

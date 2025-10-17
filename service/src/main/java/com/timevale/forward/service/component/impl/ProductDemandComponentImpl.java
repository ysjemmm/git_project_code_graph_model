package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ProductDemandGroupQueryCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.observer.event.BizDemandStatusChangeMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
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
import java.util.*;
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
            demandDetailVO.setChangeDesc(latestDescFlow.getChangeDesc());
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
        existProductDemandIds.addAll(productDemandIds);
        if (CollectionUtils.isEmpty(existProductDemandIds)) {
            log.info("更新产品需求,没有找到产品需求");
            return;
        }

        List<ProductDemandDO> productDemands = productDemandMapper.selectByIdList(existProductDemandIds);

        Map<Long, Integer> statusMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getStatus, (v1, v2) -> v2));
        Map<Long, String> nameMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getName, (v1, v2) -> v2));

        // 更新项目状态
        Integer pdStatus = ProductDemandStatusEnum.WAITING.getCode();
        if (ProjectStatusEnum.WAITING.getCode().equals(status)) {
            pdStatus = ProductDemandStatusEnum.INCLUDED.getCode();
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
        } else if (ProjectStatusEnum.SUSPEND.getCode().equals(status)) {
            pdStatus = ProductDemandStatusEnum.PJ_SUSPEND.getCode();
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
        } else if (ProjectStatusEnum.PLANING.getCode().equals(status)
                || ProjectStatusEnum.DEVING.getCode().equals(status)
                || ProjectStatusEnum.TESTING.getCode().equals(status)) {
            pdStatus = ProductDemandStatusEnum.PROGRESS.getCode();
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
        } else if (ProjectStatusEnum.RELEASED.getCode().equals(status)
                || ProjectStatusEnum.CONCLUSION.getCode().equals(status)) {
            pdStatus = ProductDemandStatusEnum.ONLINE.getCode();
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
        } else if (ProjectStatusEnum.INVALID.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
            // unlink log
            String projectName = projectMapper.get(projectId).getName();
            projectLogComponent.addLogWhenLinkOrUnlink(projectName, projectId, nameMap, null);
        }

        productDemandLogComponent.addLogAsProjectStatusChange(statusMap, pdStatus);
        updateDemandStatusAsProductStatusChange(existProductDemandIds, false);
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

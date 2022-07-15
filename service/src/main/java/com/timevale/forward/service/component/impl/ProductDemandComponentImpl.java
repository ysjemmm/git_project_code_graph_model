package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
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
    private ProductBizDemandMapper productBizDemandMapper;

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
        demandDetailVO.setProductLineVO(ProductLineCopier.INSTANCE.convert(productLineDO));

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
        if (latestDescFlow != null && FlowStatusEnum.AUDITING.getCode().equals(latestDescFlow.getStatus())) {
            demandDetailVO.setChangeDesc(latestDescFlow.getChangeDesc());
        }

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
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectId);
        if (CollectionUtils.isEmpty(exists)) {
            log.info("更新产品需求,没有找到产品需求");
            return;
        }
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        List<ProductDemandDO> productDemands = productDemandMapper.selectByIdList(existProductDemandIds);

        Map<Long, Integer> statusMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getStatus, (v1, v2) -> v2));
        Map<Long, String> nameMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getName, (v1, v2) -> v2));

        Integer pdStatus = ProductDemandStatusEnum.WAITING.getCode();
        if (ProjectStatusEnum.WAITING.getCode().equals(status) || ProjectStatusEnum.SUSPEND.getCode().equals(status)) {
            pdStatus = ProductDemandStatusEnum.INCLUDED.getCode();
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
        } else if (ProjectStatusEnum.PLANING.getCode().equals(status)
                || ProjectStatusEnum.DEVING.getCode().equals(status)
                || ProjectStatusEnum.TESTING.getCode().equals(status)) {
            pdStatus = ProductDemandStatusEnum.PROGRESS.getCode();
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
        } else if (ProjectStatusEnum.RELEASED.getCode().equals(status)) {
            pdStatus = ProductDemandStatusEnum.ONLINE.getCode();
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
        } else if (ProjectStatusEnum.INVALID.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, pdStatus, false);
            // unlink log
            String projectName = projectMapper.get(projectId).getName();
            projectLogComponent.addLogWhenLinkOrUnlink(projectName, projectId, nameMap, null);
        }

        productDemandLogComponent.addLogAsProjectStatusChange(statusMap, pdStatus);
        updateBizDemandStatusAsProductStatusChange(existProductDemandIds, false);
    }

    @Override
    public void updateBizDemandStatusAsProductStatusChange(List<Long> productDemandIds, boolean invalid) {
        if (CollectionUtils.isEmpty(productDemandIds)) {
            log.info("产品需求变化-更新业务需求,产品需求id不存在");
            return;
        }
        //invalid为true时:作废,解除产品需求和业务需求关系
        processBizDemand(productDemandIds,invalid);
    }
    private void processBizDemand(List<Long> productDemandIds, boolean invalid) {
        log.info("产品需求变化-更新业务需求:{},{}", productDemandIds, invalid);
        // 产品需求下的所有业务需求
        List<ProductBizDemandDO> bizDemands = productBizDemandMapper.getByProductDemandIds(productDemandIds);
        if (CollectionUtils.isEmpty(bizDemands)) {
            log.info("产品需求变化-更新业务需求,业务需求不存在");
            return;
        }
        // 业务需求id去重
        Map<Long, ProductBizDemandDO> bizDemandMap = bizDemands.stream()
                .collect(Collectors.toMap(ProductBizDemandDO::getBizDemandId, k -> k, (v1, v2) -> v2));
        bizDemandMap.forEach((k, v) -> {
            //被驳回和作废的业务需求不处理
            if (!BizDemandStatusEnum.REJECT.getCode().equals(v.getStatus()) && !BizDemandStatusEnum.INVALID.getCode().equals(v.getStatus())) {
                //当前业务需求下的所有产品需求
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(k);
                Integer productStatus;
                if (invalid) {
                    //如果作废:计算业务需求状态时需要过滤掉本次被解除的产品需求
                    productStatus = productDemands.stream().filter(i -> !productDemandIds.get(0).equals(i.getProductDemandId())).map(ProductBizDemandDO::getStatus)
                            .min(Comparator.comparingInt(o -> o)).orElse(null);
                } else {
                    productStatus=productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);

                }
                Integer newStatus = updateDemandStatus(productStatus, k, true);
                bizDemandLogComponent.addLogAsProductDemandStatusChange(v.getStatus(), newStatus,k);
                sendDingMsg(v.getStatus(), newStatus,v);
            }
        });
    }
    private void sendDingMsg(Integer oldStatus,Integer newStatus,ProductBizDemandDO bizDemandDO) {
        if(!Objects.equals(oldStatus,newStatus)&&BizDemandStatusEnum.statusNoNeedTodo(newStatus)){
            Date projectEndDate = bizDemandComponent.getProjectEndDate(bizDemandDO.getBizDemandId());
            log.info("发送钉钉消息,项目发布时间={},更新前状态={},更新后状态={},业务需求id={}", projectEndDate, oldStatus, newStatus ,bizDemandDO.getBizDemandId());
            messageEventPublisher.publish(new BizDemandStatusChangeMsgEvent(
                    this,
                    bizDemandDO.getBizDemandId(),
                    bizDemandDO.getSubmitManId(),
                    bizDemandDO.getName(),
                    BizDemandStatusEnum.getTextByCode(newStatus),
                    projectEndDate)
            );
        }
    }

    @Override
    public void updateBizDemandStatusWhenUnlink(Long bizDemandId, Long productDemandId) {
        List<ProductBizDemandDO> productBizDemandDos = productBizDemandMapper.getByBizDemandId(bizDemandId);
        //计算业务需求状态时需要过滤掉本次被解除的产品需求
        Integer minStatus = productBizDemandDos.stream().filter(i -> !productDemandId.equals(i.getProductDemandId())).map(ProductBizDemandDO::getStatus)
                .min(Comparator.comparingInt(o -> o)).orElse(null);
        Integer newStatus = bizDemandComponent.getBizDemandStatus(minStatus);

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        Integer oldStatus = bizDemandDO.getStatus();
        log.info("产品需求删除关联,更新前状态={},更新后状态={},产品需求id={},业务需求id={}", oldStatus, newStatus, productDemandId, bizDemandId);
        if (!Objects.equals(newStatus, oldStatus) && !BizDemandStatusEnum.statusNoNeedTodo(oldStatus)) {
            bizDemandDO.setStatus(newStatus);
            bizDemandMapper.update(bizDemandDO);
            bizDemandLogComponent.addLogAsProductDemandStatusChange(bizDemandId, oldStatus, newStatus);

            if (BizDemandStatusEnum.INCLUDE_PROJECT.getCode().equals(newStatus)
                    || BizDemandStatusEnum.PROJECTING.getCode().equals(newStatus)
                    || BizDemandStatusEnum.AVAILABLE.getCode().equals(newStatus)) {
                Date projectEndDate = bizDemandComponent.getProjectEndDate(bizDemandId);
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
    public Integer updateDemandStatus(Integer productStatus, Long demandId, boolean bizDemand) {
        Integer status = bizDemandComponent.getBizDemandStatus(productStatus);
        if(bizDemand){
            BizDemandDO bizDemandDO=new BizDemandDO();
            bizDemandDO.setId(demandId);
            bizDemandDO.setStatus(status);
            bizDemandMapper.update(bizDemandDO);
        }else {
            CustomDemandDO customDemandDO=new CustomDemandDO();
            customDemandDO.setId(demandId);
            customDemandDO.setStatus(status);
            customDemandMapper.update(customDemandDO);
        }
        return status;
    }
}

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
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Resource
    private  MessageComponent messageComponent;

//    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

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
        ProductDemandDetailVO demandDetailVO = ProductDemandCopier.INSTANCE.convert(demandDO);
        demandDetailVO.setStatusName(ProductDemandStatusEnum.getTextByCode(demandDetailVO.getStatus()));
        demandDetailVO.setPriorityName(PriorityEnum.getTextByCode(demandDetailVO.getPriority()));
        List<String> typeName = new ArrayList<>();
        if (!StringUtils.isEmpty(demandDO.getType())) {
            List<Integer> list = JSON.parseArray(demandDO.getType(), Integer.class);
            list.forEach(t -> typeName.add(ProductDemandTypeEnum.getTextByCode(t)));
            demandDetailVO.setTypes(list);
        }
        demandDetailVO.setTypeName(typeName);

        //产品线
        ProductLineDO productLineDO = productLineMapper.selectById(demandDO.getProductLineId());
        demandDetailVO.setProductLineVO(ProductLineCopier.INSTANCE.convert(productLineDO));

        //附件
        List<FileDO> fileDO = fileComponent.select(id, FileTypeEnum.PRODUCT_DEMAND.getCode());
        demandDetailVO.setFiles(FileCopier.INSTANCE.transform(fileDO));

        // 抄送人
        List<PersonDO> personDO = personComponent.select(id, PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
        demandDetailVO.setRecipients(PersonCopier.INSTANCE.transform(personDO));
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
    public void updateProductDemandStatus(Long projectId,Integer status) {
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectId);
        if (CollectionUtils.isEmpty(exists)) {
            log.info("更新产品需求,没有找到产品需求");
            return;
        }
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        if (ProjectStatusEnum.WAITING.getCode().equals(status)
                || ProjectStatusEnum.SUSPEND.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.INCLUDED.getCode());
        } else if (ProjectStatusEnum.PLANING.getCode().equals(status)
                || ProjectStatusEnum.DEVING.getCode().equals(status)
                || ProjectStatusEnum.TESTING.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.PROGRESS.getCode());
        } else if (ProjectStatusEnum.RELEASED.getCode().equals(status)) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.ONLINE.getCode());
        }
        updateBizDemandStatusAsProductStatusChange(existProductDemandIds, false);
    }

    @Override
    public void updateBizDemandStatusAsProductStatusChange(List<Long> productDemandIds, boolean bizProductDemandUnLink) {
        if (CollectionUtils.isEmpty(productDemandIds)) {
            log.info("产品需求变化-更新业务需求,产品需求id不存在");
            return;
        }
        if (bizProductDemandUnLink) {
            //解除产品需求和业务需求关系(含作废情况)
            productDemandIds.forEach(p -> {
                buildConditionBeforeUpdate(Lists.newArrayList(p), true);
            });
        } else {
            buildConditionBeforeUpdate(productDemandIds, false);
        }
    }


    private void buildConditionBeforeUpdate(List<Long> productDemandIds, boolean bizProductDemandUnLink) {
        log.info("产品需求变化-更新业务需求,产品需求id={},解除二者关联={}", productDemandIds, bizProductDemandUnLink);
        // 产品需求下的所有业务需求
        List<ProductBizDemandDO> bizDemands = productBizDemandMapper.getByProductDemandId(productDemandIds);
        if(CollectionUtils.isEmpty(bizDemands)){
            log.info("产品需求变化-更新业务需求,业务需求不存在");
            return;
        }
        // 业务需求id去重
        Map<Long, ProductBizDemandDO> bizDemandMap = bizDemands.stream()
                .collect(Collectors.toMap(ProductBizDemandDO::getBizDemandId, k -> k, (v1, v2) -> v2));
        Map<Integer, List<Long>> condition = new HashMap<>();
        bizDemandMap.forEach((k,v)->{
            //被驳回和作废的业务需求不处理
            if (!BizDemandStatusEnum.REJECT.getCode().equals(v.getStatus()) && !BizDemandStatusEnum.INVALID.getCode().equals(v.getStatus())) {
                //当前业务需求下的所有产品需求
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(k);
                if (bizProductDemandUnLink) {
                    //如果是解除关联:计算业务需求状态时需要过滤掉本次被解除的产品需求
                    Integer minStauts = productDemands.stream().filter(i -> !productDemandIds.get(0).equals(i.getProductDemandId())).map(ProductBizDemandDO::getStatus)
                            .min(Comparator.comparingInt(o -> o)).orElse(null);
                    if (minStauts == null) {
                        //业务需求只关联一个产品需求后且被解除
                        condition.computeIfAbsent(BizDemandStatusEnum.RECEIVED.getCode(), value -> new ArrayList<>()).add(k);
                    } else {
                        processUpdateStatus(condition, minStauts, k);
                    }
                } else {
                    Integer minStauts = productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);
                    if (minStauts != null) {
                        processUpdateStatus(condition, minStauts, k);
                    }
                }
            }
        });
        condition.forEach((k, v) -> {
            //更新产品需求下的所有业务需求状态
            bizDemandMapper.updateByIds(v, k);
        });
        log.info("产品需求变化-更新业务需求:产品需求id={},需要更新的业务需求状态和id={}", productDemandIds, condition);
        sendDingMsg(condition,bizDemandMap);
    }
    private void sendDingMsg(Map<Integer, List<Long>> condition,Map<Long, ProductBizDemandDO> bizDemandMap){
        condition.forEach((k, v) -> {
            if(BizDemandStatusEnum.INCLUDE_PROJECT.getCode().equals(k)
                    ||BizDemandStatusEnum.PROJECTING.getCode().equals(k)
                    ||BizDemandStatusEnum.AVAILABLE.getCode().equals(k)){
                v.forEach(a->{
                    ProductBizDemandDO bizDemand = bizDemandMap.get(a);
                    log.info("发送钉钉消息,更新前状态={},更新后状态={},业务需求id={}",bizDemand.getStatus(),k,a);
                    Date projectEndDate = bizDemandComponent.getProjectEndDate(a);
                    try {
                        messageComponent.bizDemandStatusChangeMsg(a,
                                bizDemand.getCreateManId(),
                                bizDemand.getName(),
                                BizDemandStatusEnum.getTextByCode(k),
                                projectEndDate);
                    }catch (Exception e){
                        log.error("发送钉钉消息失败",e);
                    }
                });
            }
        });
    }
    private void processUpdateStatus(Map<Integer, List<Long>> condition, Integer minStauts, Long bizDemandId) {
        if (minStauts != null && !minStauts.equals(ProductDemandStatusEnum.INVALID.getCode())) {
            if (minStauts.equals(ProductDemandStatusEnum.WAITING.getCode())
                    || minStauts.equals(ProductDemandStatusEnum.SUSPEND.getCode())) {
                condition.computeIfAbsent(BizDemandStatusEnum.RECEIVED.getCode(), v -> new ArrayList<>()).add(bizDemandId);
            } else if (minStauts.equals(ProductDemandStatusEnum.INCLUDED.getCode())) {
                condition.computeIfAbsent(BizDemandStatusEnum.INCLUDE_PROJECT.getCode(), v -> new ArrayList<>()).add(bizDemandId);
            } else if (minStauts.equals(ProductDemandStatusEnum.PROGRESS.getCode())) {
                condition.computeIfAbsent(BizDemandStatusEnum.PROJECTING.getCode(), v -> new ArrayList<>()).add(bizDemandId);
            } else if (minStauts.equals(ProductDemandStatusEnum.ONLINE.getCode())) {
                condition.computeIfAbsent(BizDemandStatusEnum.AVAILABLE.getCode(), v -> new ArrayList<>()).add(bizDemandId);
            }
        }
    }
}

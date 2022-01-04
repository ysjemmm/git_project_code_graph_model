package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

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

    @Override
    public List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition) {
        productDemandListCondition.setName(StringUtil.toLikeStr(productDemandListCondition.getName()));
        return productDemandMapper.list(productDemandListCondition);
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
    public void updateBizDemandStatusAsProductStatusChange(List<Long> productDemandIds, boolean bizProductDemandUnLink) {
        if (CollectionUtils.isEmpty(productDemandIds)) {
            log.info("产品需求变化-更新业务需求,产品需求id不存在");
            return;
        }
        if (bizProductDemandUnLink) {
            //解除产品需求和业务需求关系(含作废情况)
            productDemandIds.forEach(p -> {
                processUpdateCondition(Lists.newArrayList(p), true);
            });
        } else {
            processUpdateCondition(productDemandIds, false);
        }
    }

    @Override
    public void updateBizDemandStatusAsWhenLinkOrUnlink(Long productDemandId, Long bizDemandId, boolean bizProductDemandUnLink) {
        log.info("业务需求关联或取消关联产品需求-更新业务需求,产品需求id={},业务需求id={},解除二者关联={}", productDemandId, bizDemandId, bizProductDemandUnLink);
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if (!BizDemandStatusEnum.REJECT.getCode().equals(bizDemandDO.getStatus()) && !BizDemandStatusEnum.INVALID.getCode().equals(bizDemandDO.getStatus())) {
            List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(bizDemandDO.getId());
            Map<Integer, List<Long>> map = new HashMap<>();
            if (bizProductDemandUnLink) {
                //如果是解除关联:计算业务需求状态时需要过滤掉本次被解除的产品需求
                Integer minStauts = productDemands.stream().filter(i -> !productDemandId.equals(i.getProductDemandId())).map(ProductBizDemandDO::getStatus)
                        .min(Comparator.comparingInt(o -> o)).orElse(null);
                if (minStauts == null) {
                    //业务需求只关联一个产品需求后且被解除
                    map.computeIfAbsent(BizDemandStatusEnum.RECEIVED.getCode(), v -> new ArrayList<>()).add(bizDemandDO.getId());
                } else {
                    processUpdateStatus(map, minStauts, bizDemandDO.getId());
                }
            } else {
                Integer minStauts = productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);
                if (minStauts != null) {
                    processUpdateStatus(map, minStauts, bizDemandDO.getId());
                }
            }
            map.forEach((k, v) -> {
                //更新产品需求下的所有业务需求状态
                bizDemandMapper.updateByIds(v, k);
            });
            log.info("业务需求关联或取消关联产品需求-更新业务需求,产品需求id={},需要更新的业务需求id和状态={}", productDemandId, map);
        }
    }

    private void processUpdateCondition(List<Long> productDemandIds, boolean bizProductDemandUnLink) {
        log.info("产品需求变化-更新业务需求,产品需求id={},解除二者关联={}", productDemandIds, bizProductDemandUnLink);
        // 产品需求下的所有业务需求
        List<ProductBizDemandDO> bizDemands = productBizDemandMapper.getByProductDemandId(productDemandIds);
        Map<Integer, List<Long>> map = new HashMap<>();
        bizDemands.forEach(b -> {
            //被驳回和作废的业务需求不处理
            if (!BizDemandStatusEnum.REJECT.getCode().equals(b.getStatus()) && !BizDemandStatusEnum.INVALID.getCode().equals(b.getStatus())) {
                //当前业务需求下的所有产品需求
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(b.getBizDemandId());
                if (bizProductDemandUnLink) {
                    //如果是解除关联:计算业务需求状态时需要过滤掉本次被解除的产品需求
                    Integer minStauts = productDemands.stream().filter(i -> !productDemandIds.get(0).equals(i.getProductDemandId())).map(ProductBizDemandDO::getStatus)
                            .min(Comparator.comparingInt(o -> o)).orElse(null);
                    if (minStauts == null) {
                        //业务需求只关联一个产品需求后且被解除
                        map.computeIfAbsent(BizDemandStatusEnum.RECEIVED.getCode(), v -> new ArrayList<>()).add(b.getBizDemandId());
                    } else {
                        processUpdateStatus(map, minStauts, b.getBizDemandId());
                    }
                } else {
                    Integer minStauts = productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);
                    if (minStauts != null) {
                        processUpdateStatus(map, minStauts, b.getBizDemandId());
                    }
                }

            }
        });
        map.forEach((k, v) -> {
            //更新产品需求下的所有业务需求状态
            bizDemandMapper.updateByIds(v, k);
        });
        log.info("产品需求变化-更新业务需求:产品需求id={},需要更新的业务需求id和状态={}", productDemandIds, map);

        //发送钉钉
    }

    private void processUpdateStatus(Map<Integer, List<Long>> map, Integer minStauts, Long bizDemandId) {
        if (minStauts != null && !minStauts.equals(ProductDemandStatusEnum.INVALID.getCode())) {
            if (minStauts.equals(ProductDemandStatusEnum.WAITING.getCode())
                    || minStauts.equals(ProductDemandStatusEnum.SUSPEND.getCode())) {
                map.computeIfAbsent(BizDemandStatusEnum.RECEIVED.getCode(), v -> new ArrayList<>()).add(bizDemandId);
            } else if (minStauts.equals(ProductDemandStatusEnum.INCLUDED.getCode())) {
                map.computeIfAbsent(BizDemandStatusEnum.INCLUDE_PROJECT.getCode(), v -> new ArrayList<>()).add(bizDemandId);
            } else if (minStauts.equals(ProductDemandStatusEnum.PROGRESS.getCode())) {
                map.computeIfAbsent(BizDemandStatusEnum.PROJECTING.getCode(), v -> new ArrayList<>()).add(bizDemandId);
            } else if (minStauts.equals(ProductDemandStatusEnum.ONLINE.getCode())) {
                map.computeIfAbsent(BizDemandStatusEnum.AVAILABLE.getCode(), v -> new ArrayList<>()).add(bizDemandId);
            }
        }
    }
}

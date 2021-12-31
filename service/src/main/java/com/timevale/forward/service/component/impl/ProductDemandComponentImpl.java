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
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
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
        return productDemandMapper.list(productDemandListCondition);
    }

    @Override
    public ProductDemandDetailVO get(Long id) {
        ProductDemandDO demandDO = productDemandMapper.get(id);
        ProductDemandDetailVO demandDetailVO = ProductDemandCopier.INSTANCE.convert(demandDO);
        demandDetailVO.setStatusName(ProductDemandStatusEnum.getTextByCode(demandDetailVO.getStatus()));
        demandDetailVO.setPriorityName(PriorityEnum.getTextChineseByCode(demandDetailVO.getPriority()));
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
        // 找出所有产品需求下的所有业务需求
        List<ProductBizDemandDO> bizDemands = productBizDemandMapper.getByProductDemandId(productDemandIds);
        Map<Integer, List<Long>> map = new HashMap<>();
        bizDemands.forEach(b -> {
            //被驳回和作废的业务需求不处理
            if (!BizDemandStatusEnum.REJECT.getCode().equals(b.getStatus()) && !BizDemandStatusEnum.INVALID.getCode().equals(b.getStatus())) {
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(b.getBizDemandId());
                if (bizProductDemandUnLink && productDemands.size() == 1) {
                    //业务需求只关联一个产品需求后被解除
                    map.computeIfAbsent(BizDemandStatusEnum.RECEIVED.getCode(), v -> new ArrayList<>()).add(b.getBizDemandId());
                } else {
                    // 关联多个产品需求,找到每个业务需求下所有产品需求的最小状态值.
                    Integer minStauts = productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);
                    if (minStauts != null && !minStauts.equals(ProductDemandStatusEnum.INVALID.getCode())) {
                        if (minStauts.equals(ProductDemandStatusEnum.WAITING.getCode())
                                || minStauts.equals(ProductDemandStatusEnum.SUSPEND.getCode())) {
                            map.computeIfAbsent(BizDemandStatusEnum.RECEIVED.getCode(), v -> new ArrayList<>()).add(b.getBizDemandId());
                        } else if (minStauts.equals(ProductDemandStatusEnum.INCLUDED.getCode())) {
                            map.computeIfAbsent(BizDemandStatusEnum.INCLUDE_PROJECT.getCode(), v -> new ArrayList<>()).add(b.getBizDemandId());
                        } else if (minStauts.equals(ProductDemandStatusEnum.PROGRESS.getCode())) {
                            map.computeIfAbsent(BizDemandStatusEnum.PROJECTING.getCode(), v -> new ArrayList<>()).add(b.getBizDemandId());
                        } else if (minStauts.equals(ProductDemandStatusEnum.ONLINE.getCode())) {
                            map.computeIfAbsent(BizDemandStatusEnum.AVAILABLE.getCode(), v -> new ArrayList<>()).add(b.getBizDemandId());
                        }
                    }
                }

            }
        });
        map.forEach((k, v) -> {
            //根据业务需求下所有产品需求的最小状态值.批量更新业务需求状态
            bizDemandMapper.updateByIds(v, k);
        });
        log.info("产品需求变化-更新业务需求:map={}", map);
        //发送钉钉
    }

//    private void updateDemandStatusIfNecessary(Long projectId, Integer projectStatus) {
//        List<ProjectProductDemandDO> productDemandDO = projectProductDemandMapper.getByProjectId(projectId);
//        log.info("项目关联的产品需求:productDemandDO={}", productDemandDO);
//        productDemandDO.forEach(p -> {
//            if (!ProjectStatusEnum.INVALID.getCode().equals(projectStatus)) {
//                ProductDemandDO productDemand = new ProductDemandDO();
//                productDemand.setId(p.getProductDemandId());
//                if (ProjectStatusEnum.WAITING.getCode().equals(projectStatus)
//                        || ProjectStatusEnum.SUSPEND.getCode().equals(projectStatus)) {
//                    productDemand.setStatus(ProductDemandStatusEnum.INCLUDED.getCode());
//
//                } else if (ProjectStatusEnum.PLANING.getCode().equals(projectStatus)
//                        || ProjectStatusEnum.DEVING.getCode().equals(projectStatus)
//                        || ProjectStatusEnum.TESTING.getCode().equals(projectStatus)) {
//                    productDemand.setStatus(ProductDemandStatusEnum.PROGRESS.getCode());
//
//                } else if (ProjectStatusEnum.RELEASED.getCode().equals(projectStatus)) {
//                    productDemand.setStatus(ProductDemandStatusEnum.ONLINE.getCode());
//                }
//                update(productDemand);
//                log.info("项目关联的产品需求状态更新成功:demandDO={}", productDemand);
//
//                if (ProductDemandStatusEnum.INCLUDED.getCode().equals(productDemand.getStatus())
//                        || ProductDemandStatusEnum.PROGRESS.getCode().equals(productDemand.getStatus())) {
//                    ProductBizDemandCondition condition = ProductBizDemandCondition.builder().productDemandId(p.getProductDemandId()).isDeleted(false).build();
//                    // 一个产品需求下的业务需求
//                    List<ProductBizDemandDO> bizDemand = productBizDemandMapper.select(condition);
//                    bizDemand.forEach(a -> {
//                        condition.setProductDemandId(null);
//                        condition.setBizDemandId(a.getBizDemandId());
//                        // 该业务需求下的产品需求
//                        List<ProductBizDemandDO> demand = productBizDemandMapper.getByBizDemandId(a.getBizDemandId());
//                        BizDemandDO bizDemandDO = new BizDemandDO();
//                        bizDemandDO.setId(a.getBizDemandId());
//                        if (demand.size() > 1) {
//                            bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
//                            // 关联的业务需求，存在其他关联的产品需求且为待排期时 业务需求状态改为：已接收
//                            List<ProductBizDemandDO> productBizDemandDOS = demand.stream()
//                                    .filter((b) ->
//                                            (ProductDemandStatusEnum.WAITING.getCode().equals(b.getStatus()) && !p.getProductDemandId().equals(b.getProductDemandId()))
//                                    ).collect(Collectors.toList());
//                            log.info("被关联的业务需求,存在其他关联的产品需求:productDemandIds={}", productBizDemandDOS);
//                            if (!CollectionUtils.isEmpty(productBizDemandDOS)) {
//                                bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
//                                bizDemandMapper.update(bizDemandDO);
//                            }
//
//                        } else {
//                            //关联的业务需求，仅关联该产品需求时
//                            bizDemandDO.setStatus(productDemand.getStatus());
//                            bizDemandMapper.update(bizDemandDO);
//                        }
//                        log.info("产品需求关联的业务需求状态更新成功:bizDemandDO={}", bizDemandDO);
//                    });
//                }
//            }
//        });
//    }
}

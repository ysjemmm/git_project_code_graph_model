package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
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
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition) {
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

    /**
     * 当项目状态发生变化时(满足条件时:有关联的产品需求且状态不是已作废)需要改变产品需求状态
     * 已列入项目：该产品需求所关联的项目状态为待启动
     * 项目进行中：该产品需求所关联的项目状态为规划中、研发中、测试中
     * 已完成上线：该产品需求所关联的项目状态为已发布
     *
     * @param projectId     项目id
     * @param projectStatus 项目状态
     */
    @Override
    public void updateDemandStatusIfNecessary(Long projectId, Integer projectStatus) {
        List<ProjectProductDemandDO> productDemandDO = projectProductDemandComponent.getByProjectId(projectId);
        log.info("项目关联的产品需求:productDemandDO={}", productDemandDO);
        productDemandDO.forEach(p -> {
            if (!ProjectStatusEnum.INVALID.getCode().equals(projectStatus)) {
                ProductDemandDO demandDO = new ProductDemandDO();
                demandDO.setId(p.getProductDemandId());
                if (ProjectStatusEnum.WAITING.getCode().equals(projectStatus)
                        || ProjectStatusEnum.SUSPEND.getCode().equals(projectStatus)) {
                    demandDO.setStatus(ProductDemandStatusEnum.INCLUDED.getCode());

                } else if (ProjectStatusEnum.PLANING.getCode().equals(projectStatus)
                        || ProjectStatusEnum.DEVING.getCode().equals(projectStatus)
                        || ProjectStatusEnum.TESTING.getCode().equals(projectStatus)) {
                    demandDO.setStatus(ProductDemandStatusEnum.PROGRESS.getCode());

                } else if (ProjectStatusEnum.RELEASED.getCode().equals(projectStatus)) {
                    demandDO.setStatus(ProductDemandStatusEnum.ONLINE.getCode());
                }
                update(demandDO);
                log.info("项目关联的产品需求状态更新成功:demandDO={}", demandDO);

                if (ProductDemandStatusEnum.INCLUDED.getCode().equals(demandDO.getStatus())
                        || ProductDemandStatusEnum.PROGRESS.getCode().equals(demandDO.getStatus())) {
                    ProductBizDemandCondition condition = ProductBizDemandCondition.builder().productDemandId(p.getProductDemandId()).isDeleted(false).build();
                    // 一个产品需求下的业务需求
                    List<ProductBizDemandDO> bizDemand = productBizDemandMapper.select(condition);
                    bizDemand.forEach(a -> {
                        condition.setProductDemandId(null);
                        condition.setBizDemandId(a.getBizDemandId());
                        // 该业务需求下的产品需求
                        List<ProductBizDemandDO> productDemand = productBizDemandMapper.select(condition);
                        BizDemandDO bizDemandDO = new BizDemandDO();
                        bizDemandDO.setId(a.getBizDemandId());
                        if (productDemand.size() > 1) {
                            bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
                            // 关联的业务需求，存在其他关联的产品需求时,更新为待排期
                            List<Long> productDemandIds = productDemand.stream().map(ProductBizDemandDO::getProductDemandId)
                                    .filter((b) -> !p.getProductDemandId().equals(b)).collect(Collectors.toList());
                            log.info("被关联的业务需求,存在其他关联的产品需求:productDemandIds={}", productDemandIds);
                            productDemandMapper.updateByIds(productDemandIds,ProductDemandStatusEnum.WAITING.getCode());

                        } else {
                            //关联的业务需求，仅关联该产品需求时
                            bizDemandDO.setStatus(demandDO.getStatus());
                        }
                        bizDemandMapper.update(bizDemandDO);
                        log.info("产品需求关联的业务需求状态更新成功:bizDemandDO={}", bizDemandDO);
                    });
                }
            }
        });
    }


}

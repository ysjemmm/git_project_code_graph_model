package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProductDemandComponent;
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
        List<String> typeName=new ArrayList<>();
        if(!StringUtils.isEmpty(demandDO.getType())){
            List<Integer> list = JSON.parseArray(demandDO.getType(), Integer.class);
            list.forEach(t->typeName.add(ProductDemandTypeEnum.getTextByCode(t)));
        }
        demandDetailVO.setTypeName(typeName);

        //产品线
        ProductLineDO productLineDO = productLineMapper.selectById(demandDO.getProductLineId());
        demandDetailVO.setProductLineVO(ProductLineCopier.INSTANCE.convert(productLineDO));

        //附件
        List<FileDO> fileDO = fileComponent.select(id, FileTypeEnum.PRODUCT_DEMAND.getCode());
        demandDetailVO.setFiles(FileCopier.INSTANCE.transform(fileDO));

        // 抄送人
        List<PersonDO> personDO= personComponent.select(id, PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
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


}

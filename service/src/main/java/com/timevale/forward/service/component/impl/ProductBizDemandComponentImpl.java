package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.service.component.ProductBizDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProductBizDemandComponentImpl implements ProductBizDemandComponent {

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Override
    public void update(ProductBizDemandDO productBizDemandDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        productBizDemandDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        productBizDemandDO.setModifyManId(userInfo.getId());
        productBizDemandMapper.update(productBizDemandDO);
    }


}

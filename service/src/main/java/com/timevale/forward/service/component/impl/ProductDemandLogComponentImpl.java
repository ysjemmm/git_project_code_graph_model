package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.service.component.BizChangeLogComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProductDemandLogComponentImpl implements BizChangeLogComponent<ProductDemandDO> {

    @Resource
    private BugLogMapper bugLogMapper;

    @Override
    public void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) {


    }
}

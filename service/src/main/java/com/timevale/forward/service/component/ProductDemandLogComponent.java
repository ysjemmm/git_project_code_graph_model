package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProductDemandDO;

import java.util.Map;

public interface ProductDemandLogComponent {

    void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) ;

    void addLogWhenStatusChange(Map<Long, Integer> oldStautsMap,  Integer newStauts) ;
}

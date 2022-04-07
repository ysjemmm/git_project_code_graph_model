package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProductDemandDO;

import java.util.Map;

public interface ProductDemandLogComponent {

    void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) ;

    void addLogAsProjectStatusChange(Map<Long, Integer> statusMap,  Integer newStauts) ;
}

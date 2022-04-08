package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProductDemandDO;

import java.util.Map;

public interface ProductDemandLogComponent {

    void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) ;

    void addLogAsProjectStatusChange(Map<Long, Integer> statusMap,  Integer newStauts) ;

    void addLogWhenStatusChange(Integer oldStatus,Integer newStatus,Long id,String action) ;

    void addLogWhenLinkOrUnlink(String name, Long id,Map<Long, String> bdNameMap,String linkOrUnlink)  ;
}

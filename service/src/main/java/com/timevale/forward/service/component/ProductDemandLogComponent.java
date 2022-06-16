package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProductDemandDO;

import java.util.List;
import java.util.Map;

public interface ProductDemandLogComponent {

    void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) ;

    BizChangeLogDO getLog(String oldValue, String newValue, Long id, String field, Boolean active);

    void batchAddLog(List<BizChangeLogDO> bizChangeLogDOList);

    void addLogAsProjectStatusChange(Map<Long, Integer> statusMap,  Integer newStauts) ;

    void addLogWhenStatusChange(Integer oldStatus,Integer newStatus,Long id,String action) ;

    void addLogWhenLinkOrUnlink(String name, Long id,Map<Long, String> bdNameMap,String linkOrUnlink)  ;

    void addLogWhenLinkOrUnlinkTrackEvent(Long id,List<String> trackEventName,String linkOrUnlink)  ;
}

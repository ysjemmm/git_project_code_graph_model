package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandOwnerDO;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;

import java.util.List;
import java.util.Map;

public interface ProductDemandLogComponent {

    void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) ;

    void addLogWhenModifyResourcePlan(Long productDemandId, List<ProductDemandOwnerDO> oldOwners, List<ProductDemandOwnerDO> newOwners);

    BizChangeLogDO getLog(String oldValue, String newValue, Long id, String field, Boolean active);

    void batchAddLog(List<BizChangeLogDO> bizChangeLogDOList);

    void addLogAsProjectStatusChange(Map<Long, Integer> statusMap,  Integer newStauts) ;

    void addLogWhenStatusChange(Integer oldStatus,Integer newStatus,Long id,String action) ;

    void addLogWhenLinkOrUnlink(String name, Long id, Map<Long, String> bdNameMap, String linkOrUnlink, BizChangeLogTypeEnum bizChangeLogTypeEnum)  ;

    void addLogWhenLinkOrUnlink(String name, Long id, Map<Long, String> bdNameMap, String linkOrUnlink)  ;

    void addLogWhenLinkOrUnlinkCustomDemand(String name, Long id, Map<Long, String> bdNameMap, String linkOrUnlink)  ;

    void addLogWhenLinkOrUnlinkTrackEvent(Long id,List<String> trackEventName,String linkOrUnlink)  ;
}

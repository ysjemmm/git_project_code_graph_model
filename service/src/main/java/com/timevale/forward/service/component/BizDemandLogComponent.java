package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizDemandDO;

import java.util.List;
import java.util.Map;

public interface BizDemandLogComponent{

    void addLogWhenStatusChange(Integer oldStatus, Integer newStatus, Long id, String action) ;

    void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) ;

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String action, String filed) ;

    void addLogWhenBizDemandInvalid(Long bizDemandId);

    void addLogWhenBizDemandLinkProductDemand(Long bizDemandId, List<Long> productDemandIdList);

    void addLogWhenBizDemandUnLinkProductDemand(Long bizDemandId, Long productDemandId);

    void addLogAsProductDemandStatusChange(Map<Long, Integer> oldStatusMap,  Map<Integer, List<Long>> newStatusMap) ;
}

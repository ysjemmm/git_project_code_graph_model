package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizDemandDO;

import java.util.List;
import java.util.Map;

public interface BizDemandLogComponent{

    void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) ;

    void addLogWhenStatusChange(Map<Long, Integer> oldStautsMap,  Map<Integer, List<Long>> newStautsMap) ;
}

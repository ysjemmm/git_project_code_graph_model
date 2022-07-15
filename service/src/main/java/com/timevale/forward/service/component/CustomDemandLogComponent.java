package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizChangeLogDO;

import java.util.List;
import java.util.Map;

public interface CustomDemandLogComponent {


    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    void addLogWhenCustomDemandLinkProductDemand(Long customDemandId, List<Long> productDemandIdList);

    void addLogWhenCustomDemandUnLinkProductDemand(Long customDemandId, Long productDemandId);

    void addLogAsProductDemandStatusChange(Map<Long, Integer> oldStatusMap,  Map<Integer, List<Long>> newStatusMap) ;

    void addLogAsProductDemandStatusChange(Long id,Integer oldStatus, Integer newStatus);

    BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    BizChangeLogDO buildLogWhenPublishDateChange(String oldValue, String newValue, Long id);
}

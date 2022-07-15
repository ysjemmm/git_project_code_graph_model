package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;

import java.util.List;

public interface BizDemandLogComponent{

    void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) ;

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    void addLogWhenBizDemandInvalid(Long bizDemandId);

    void addLogWhenBizDemandLinkProductDemand(Long bizDemandId, List<Long> productDemandIdList);

    void addLogWhenBizDemandUnLinkProductDemand(Long bizDemandId, Long productDemandId);

    void addLogAsProductDemandStatusChange(Integer oldStatus,  Integer newStatus,Long id) ;

    void addLogAsProductDemandStatusChange(Long id,Integer oldStatus, Integer newStatus);

    BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    BizChangeLogDO buildLogWhenPublishDateChange(String oldValue, String newValue, Long id);
}

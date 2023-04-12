package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;

import java.util.Collection;

public interface BizDemandLogComponent{

    void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) ;

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action, String identity);

    void addLogWhenBizDemandInvalid(Long bizDemandId);

    void linkPd(Long bizDemandId, Collection<Long> productDemandIdList);

    void unlinkPd(Long bizDemandId, Long productDemandId);

    void addLogAsProductDemandStatusChange(Integer oldStatus,  Integer newStatus,Long id,Integer type) ;

    BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    BizChangeLogDO buildLogWhenPublishDateChange(String oldValue, String newValue, Long id);

    BizChangeLogDO buildLogWhenPublishDateChange(String oldValue, String newValue, Long id,Integer type);

    BizChangeLogDO buildLogWhenUpdateFiles(String oldValue, String newValue, Long id,String action);
}

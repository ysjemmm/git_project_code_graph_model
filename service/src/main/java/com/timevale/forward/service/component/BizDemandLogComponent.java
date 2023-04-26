package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;

import java.util.Collection;

public interface BizDemandLogComponent{

    void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) ;

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action, String identity);

    void addLogWhenBizDemandInvalid(Long bizDemandId);

    void linkPd(Long bizDemandId, Collection<Long> productDemandIdList);

    void unlinkPd(Long bizDemandId, Long productDemandId);

    void linkProject(ProjectDO project, Collection<BizDemandDO> bizDemands);

    void unlinkProject(ProjectDO project, Collection<BizDemandDO> bizDemands);

    void addLogAsProductDemandStatusChange(Integer oldStatus,  Integer newStatus,Long id,Integer type) ;

    void addLogsAsProjectStatusChange(Integer oldStatus, Integer newStatus, Collection<Long> ids);

    BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    BizChangeLogDO buildLogWhenPublishDateChange(String oldValue, String newValue, Long id);

    BizChangeLogDO buildLogWhenStatusChange(String oldValue, String newValue, Long id);

    BizChangeLogDO buildLogWhenPublishDateChange(String oldValue, String newValue, Long id,Integer type);

    BizChangeLogDO buildLogWhenUpdateFiles(String oldValue, String newValue, Long id,String action);

}

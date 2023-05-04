package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;

import java.util.Collection;

public interface BizDemandLogComponent{


    void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) ;

    /**
     * 记录变更日志
     *
     * @param oldValue 旧值
     * @param newValue 新值
     * @param id       id
     * @param field    字段名
     * @param active   是否主动触发变更
     */
    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active);

    /**
     * 修改数据时添加日志
     *
     * @param oldValue 旧值
     * @param newValue 新值
     * @param id       id
     * @param field    字段名
     * @param active   是否主动触发变更
     * @param action   触发动作
     */
    void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action);

    /**
     * 修改数据时添加日志
     *
     * @param oldValue 旧值
     * @param newValue 新值
     * @param id       id
     * @param field    字段名
     * @param active   是否主动触发变更
     * @param action   触发动作
     * @param identity 数据标识
     */
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

    void updateReceiveMan(Long bizDemandId, String oldReceiveMan, String newReceiveMan);

}

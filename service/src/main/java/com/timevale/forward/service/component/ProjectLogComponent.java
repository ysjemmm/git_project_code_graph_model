package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectDO;

import java.util.Map;

public interface ProjectLogComponent{

    void addLogWhenModifyData(ProjectDO oldObj, ProjectDO newObj) ;

    void addLogWhenStatusChange(Integer oldStatus,Integer newStatus,Long id,String action) ;

    void addLogWhenLinkOrUnlink(Map<Long, String> pdNameMap, String name, Long id,String linkOrUnlink)  ;
}

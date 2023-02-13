package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectDO;

import java.util.Map;

public interface ProjectLogComponent{

    void addLogWhenModifyData(ProjectDO oldObj, ProjectDO newObj) ;

    void addLogWhenSimpleModifyData(ProjectDO oldObj, ProjectDO newObj) ;

    void addLogWhenStatusChange(Integer oldStatus,Integer newStatus,Long id,String action) ;

    void addLogWhenLinkOrUnlink(String name, Long id,Map<Long, String> pdNameMap,String linkOrUnlink)  ;

    void addLogWhenContentChange(String oldValue,String newValue,Long id,String field) ;

    void addLogWhenContentChange(String oldValue,String newValue,Long id,String field, String action) ;
}

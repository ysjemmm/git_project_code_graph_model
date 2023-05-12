package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectDO;

import java.util.Date;
import java.util.Map;

public interface ProjectLogComponent{

    void addLogWhenModifyData(ProjectDO oldObj, ProjectDO newObj) ;

    void addLogWhenSimpleModifyData(ProjectDO oldObj, ProjectDO newObj) ;

    void addLogWhenStatusChange(Integer oldStatus,Integer newStatus,Long id,String action) ;

    void addLogWhenLinkOrUnlink(String name, Long id,Map<Long, String> pdNameMap,String linkOrUnlink)  ;

    void addLogWhenContentChange(String oldValue,String newValue,Long id,String field) ;

    void addLogWhenContentChange(String oldValue,String newValue,Long id,String field, String action) ;

    void addAppendChildLog(Long id, String childName);

    void addAttachParentLog(Long id, String parentName);

    void addDeleteChildLog(Long id, String childName);

    void addDetachParentLog(Long id, String parentName);

    void addNewProjectMemberLog(Long projectId, String members);

    void addDeleteProjectMemberLog(Long projectId, String members);

    void addConclusion(Long projectId, Integer oldStatus, Integer newStatus, Date conclusionDate);

    void addLogWhenSystemCreate(Long projectId, String text);

    void status(Long projectId, Integer oldStatus, Integer newStatus);

    void conclusionDate(Long projectId, Date conclusionDate);
}

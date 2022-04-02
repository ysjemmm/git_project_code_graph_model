package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectDO;

public interface ProjectLogComponent{

    void addLogWhenModifyData(ProjectDO oldObj, ProjectDO newObj) ;
}

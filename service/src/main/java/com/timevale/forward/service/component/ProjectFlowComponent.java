package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.mandarin.common.query.QueryBase;

import java.util.List;

public interface ProjectFlowComponent {

    void  updateFlowInfo(String processInstanceId);

    List<ProjectFlowDO> flushCompleteFlow(QueryBase queryBase);
}

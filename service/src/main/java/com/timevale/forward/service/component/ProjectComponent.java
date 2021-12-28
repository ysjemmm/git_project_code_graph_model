package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.mandarin.common.result.PageQueryResult;

public interface ProjectComponent {
    /**
     *
     * @param projectListCondition 查询条件
     * @return 列表
     */
    BaseResult<PageQueryResult<ProjectVO>> page (ProjectListCondition projectListCondition);

}

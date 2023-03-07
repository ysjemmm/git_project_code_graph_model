package com.timevale.forward.service.integration.publish;

import com.timevale.forward.dal.dto.DevopsProjectDTO;
import com.timevale.forward.dal.dto.PublishPlanResultDTO;
import com.timevale.forward.facade.api.query.PublishPlanQueryList;

/**
 * @author xingyun
 * @date 2022/5/12 10:57
 */
public interface PublishPlatformClient {

    /**
     * 查询发布计划
     * @param publishPlanQueryListt 发起参数
     * @return 结果
     */
    PublishPlanResultDTO list(PublishPlanQueryList publishPlanQueryListt);

    DevopsProjectDTO getProject(String projectSign);

}

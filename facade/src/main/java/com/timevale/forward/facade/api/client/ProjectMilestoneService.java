package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectMilestoneService {

    /**
     * 添加里程碑
     */
    BaseResult<Void> add(ProjectMilestoneAddReq projectMilestoneAddReq);

    /**
     * 里程碑列表查询
     */
    BaseResult<List<ProjectMilestoneVO>> listMilestones(Long projectId);

}

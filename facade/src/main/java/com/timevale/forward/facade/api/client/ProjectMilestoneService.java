package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectMilestoneActionDelReq;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneListVO;
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
    BaseResult<ProjectMilestoneListVO> listMilestones(Long projectId);

    /**
     * 查询项目关联里程碑列表（包含子项目）
     */
    BaseResult<List<ProjectMilestoneVO>> getRelatedMilestones(Long projectId);

    /**
     * 里程碑删除
     */
    BaseResult<Void> deleteMilestone(Long milestoneId);

    /**
     * 删除行动
     *
     * @param actionDelReq 行动删除请求
     * @return {@link BaseResult}<{@link Void}>
     */
    BaseResult<Void> delAction(ProjectMilestoneActionDelReq actionDelReq);
}

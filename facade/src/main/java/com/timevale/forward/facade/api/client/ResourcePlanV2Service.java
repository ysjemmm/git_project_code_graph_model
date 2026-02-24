package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.request.ResourcePlanV2SaveReq;
import com.timevale.forward.facade.api.result.ResourcePlanV2VO;

/**
 * 资源规划V2服务接口
 *
 * @author kiro
 * @date 2026-02-24
 */
public interface ResourcePlanV2Service {

    /**
     * 查询分组资源规划（V2，按人拆分人天）
     */
    BaseResult<ResourcePlanV2VO> getResourcePlanV2(Long bizDomainGroupId, Long productDemandGroupId);

    /**
     * 批量保存资源规划（V2，按人拆分人天）
     */
    BaseResult<Boolean> saveResourcePlanV2(ResourcePlanV2SaveReq req);
}

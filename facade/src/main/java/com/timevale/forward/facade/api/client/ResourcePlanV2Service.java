package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ResourcePlanV2SaveReq;
import com.timevale.forward.facade.api.result.ResourcePlanV2VO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 资源规划V2服务接口
 *
 * @author kiro
 * @date 2026-02-24
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ResourcePlanV2Service {

    /**
     * 查询分组资源规划（V2，按人拆分人天）
     */
    BaseResult<ResourcePlanV2VO> getResourcePlanV2(Long bizDomainGroupId, Long productDemandGroupId);

    /**
     * 批量保存资源规划（V2，按人拆分人天）
     */
    BaseResult<Boolean> saveResourcePlanV2(ResourcePlanV2SaveReq req);

    /**
     * 查询单条需求的资源分配（V2，按人拆分人天）
     */
    BaseResult<List<ResourcePlanV2VO.OwnerTimeItem>> getOwnerTimesByDemandId(Long productDemandId);

    /**
     * 保存单条需求的资源分配（V2，按人拆分人天）
     */
    BaseResult<Boolean> saveOwnerTimesByDemandId(ResourcePlanV2SaveReq req);

    /**
     * 批量查询分组资源人天汇总
     */
    BaseResult<Map<Long, Map<String, java.math.BigDecimal>>> getGroupTimeSummary(List<Long> groupIds);
}

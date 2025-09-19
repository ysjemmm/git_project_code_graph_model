package com.timevale.forward.service.integration.publish;

import com.timevale.forward.dal.dto.DevopsProjectDTO;
import com.timevale.forward.dal.dto.PublishPlanResultDTO;
import com.timevale.forward.facade.api.query.PublishPlanQueryList;

import java.util.Map;

/**
 * @author xingyun
 * @date 2022/5/12 10:57
 */
public interface PublishPlatformClient {

    /**
     * 查询发布计划
     *
     * @param publishPlanQueryListt 发起参数
     * @return 结果
     */
    PublishPlanResultDTO list(PublishPlanQueryList publishPlanQueryListt);

    DevopsProjectDTO getProject(String projectSign);

    /**
     * 获取发布火车列表
     *
     * @param params 查询参数
     * @return 发布火车列表数据
     */
    Map<String, Object> getTrainList(Map<String, Object> params);

    /**
     * 获取发布火车基础信息
     * @param trainId 发布火车ID
     * @return 发布火车基础信息
     */
    Map<String, Object> getTrainDetail(Integer trainId);

    /**
     * 获取发布火车-发布详情
     * @param trainId 发布火车ID
     * @param params 查询参数
     * @return 发布火车-发布详情
     */
    Map<String, Object> getTrainPublishPage(Integer trainId, Map<String, Object> params);
}

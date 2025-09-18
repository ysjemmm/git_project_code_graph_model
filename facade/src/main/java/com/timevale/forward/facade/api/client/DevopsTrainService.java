package com.timevale.forward.facade.api.client;

import com.timevale.forward.facade.api.MagicValue;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.Map;

/**
 * Devops发布火车服务接口
 *
 * @author dijiu
 * @date 2025/9/17
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface DevopsTrainService {

    /**
     * 获取发布火车列表
     *
     * @param params 查询参数
     * @return 发布火车列表数据
     */
    BaseResult<PageQueryResult<Map<String, Object>>> getTrainList(Map<String, Object> params);

    /**
     * 获取发布火车详情
     *
     * @param trainId 发布火车ID
     * @return 发布火车详情数据
     */
    BaseResult<Map<String, Object>> getTrainDetail(Integer trainId);

    /**
     * 关联一个批量发布
     * @param trainId 批量发布Id
     * @param projectId 产研项目Id
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> linkProjectToTrain(Integer trainId, Integer projectId);

    /**
     * 取消关联一个批量发布
     * @param id 关联表主健id
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> unlinkProjectFromTrain(Integer id);

    /**
     * 获取产研项目内的批量发布列表
     * @param projectId 产研项目Id
     * @param page 偏移量
     * @param pageSize 每页个数
     * @return <PageQueryResult<Map<String, Object>>>
     */
    BaseResult<PageQueryResult<Map<String, Object>>> getTrainListByProjectId(
            Integer projectId,
            Integer page,
            Integer pageSize);
}

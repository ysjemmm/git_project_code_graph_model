package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.DevopsTrainService;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.enums.BaseResultCodeEnum;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Devops发布火车服务实现
 *
 * @author xingyun
 * @date 2025/9/17
 */
@Slf4j
@LogPoint
@RestService
public class DevopsTrainServiceImpl implements DevopsTrainService {

    @Resource
    private PublishPlatformClient platformClient;

    /**
     * 获取发布火车列表
     *
     * @param params 查询参数
     * @return 发布火车列表数据
     */
    @Override
    public BaseResult<PageQueryResult<Map<String, Object>>> getTrainList(Map<String, Object> params) {
        try {
            Map<String, Object> result = platformClient.getTrainList(params);
            if (result == null) {
                return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车列表失败");
            }

            // 解析分页数据
            PageQueryResult<Map<String, Object>> pageResult = new PageQueryResult<>();
            pageResult.setResultList((java.util.List<Map<String, Object>>) result.get("list"));
            pageResult.setTotalItems((Integer) result.get("count"));

            return BaseResult.success(pageResult);
        } catch (Exception e) {
            log.error("获取发布火车列表异常", e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车列表异常: " + e.getMessage());
        }
    }

    /**
     * 获取发布火车详情
     *
     * @param trainId 发布火车ID
     * @return 发布火车详情数据
     */
    @Override
    public BaseResult<Map<String, Object>> getTrainDetail(String trainId) {
        try {
            Map<String, Object> result = platformClient.getTrainDetail(trainId);
            if (result == null) {
                return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车详情失败");
            }

            // 直接返回结果数据
            return BaseResult.success(result);
        } catch (Exception e) {
            log.error("获取发布火车详情异常, trainId: {}", trainId, e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车详情异常: " + e.getMessage());
        }
    }
}
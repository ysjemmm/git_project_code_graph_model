package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.DevopsReq;
import com.timevale.forward.facade.api.result.DevopsAppVO;
import com.timevale.forward.facade.api.result.DevopsProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface DevopsAppService {

    /**
     * 获取已经关联的项目列表
     *
     * @param projectId 项目id
     * @return {@link BaseResult}<{@link Integer}>
     */
    BaseResult<List<DevopsProjectVO>> getDevopsProjects(Long projectId);

    /**
     * 添加发布平台项目关联
     *
     * @param devopsReq devops点播
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> addDevopsProject(DevopsReq devopsReq);

    /**
     * 删除发布平台项目关联
     *
     * @param devopsReq devops点播
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> deleteDevopsProject(DevopsReq devopsReq);

    /**
     * 更新勾选统计
     *
     * @param id id
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> updateStatFlag(Long id);

    /**
     * 分页查询-发布平台项目下属应用数据
     *
     * @param projectId 项目id
     * @return {@link BaseResult}<{@link QueryResultVO}<{@link DevopsAppVO}>>
     */
    BaseResult<QueryResultVO<DevopsAppVO>> listDevopsApps(Long projectId);
}

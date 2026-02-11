package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ColumnHideGetReq;
import com.timevale.forward.facade.api.request.ColumnHideModifyReq;
import com.timevale.forward.facade.api.result.ColumnHideVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author by YangXu
 * @date 2022/06/24 09:50
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ColumnHideService {
    /**
     * 查询
     *
     * @param columnHideGetReq 查询请求
     * @return {@link BaseResult}<{@link ColumnHideVO}>
     */
    BaseResult<ColumnHideVO> get(ColumnHideGetReq columnHideGetReq);

    /**
     * 更新
     *
     * @param columnHideModifyReq 修改请求
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> update(ColumnHideModifyReq columnHideModifyReq);

    /**
     * 获取全局配置（不区分用户，所有人共享同一份数据）
     *
     * @param columnHideGetReq 查询请求（model + tabType）
     * @return {@link BaseResult}<{@link ColumnHideVO}>
     */
    BaseResult<ColumnHideVO> getGlobal(ColumnHideGetReq columnHideGetReq);

    /**
     * 更新全局配置（不区分用户，所有人共享同一份数据）
     *
     * @param columnHideModifyReq 修改请求（model + tabType + content）
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> updateGlobal(ColumnHideModifyReq columnHideModifyReq);
}

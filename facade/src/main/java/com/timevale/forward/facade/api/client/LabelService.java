package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.LabelQueryList;
import com.timevale.forward.facade.api.request.LabelAddReq;
import com.timevale.forward.facade.api.request.LabelModifyReq;
import com.timevale.forward.facade.api.result.LabelCategoryVO;
import com.timevale.forward.facade.api.result.LabelDetailVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface LabelService {

    /**
     * 列表
     *
     * @return 列表
     */
    BaseResult<PageQueryResult<LabelCategoryVO>> list(LabelQueryList labelQueryList);

    /**
     * 查看
     *
     * @param labelId labelId
     * @return 详情信息
     */
    BaseResult<LabelDetailVO> get(Long labelId);

    /**
     * 查看
     *
     * @param labelId labelId
     * @return 详情信息
     */
    BaseResult<Boolean> delete(Long labelId);


    /**
     * 新增
     *
     * @param labelAddReq 标签新增
     * @return Boolean
     */
    BaseResult<Boolean> add(LabelAddReq labelAddReq);

    /**
     * 新增
     *
     * @param labelModifyReq 标签修改
     * @return Boolean
     */
    BaseResult<Boolean> modify(LabelModifyReq labelModifyReq);


}

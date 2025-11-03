package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ModelQueryList;
import com.timevale.forward.facade.api.request.ModelAddReq;
import com.timevale.forward.facade.api.request.ModelModifyReq;
import com.timevale.forward.facade.api.result.ModelFormFieldVO;
import com.timevale.forward.facade.api.result.ModelVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/13 17:02
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ModelService {

    /**
     * 获取表单字段列表
     *
     * @return 列表
     */
    BaseResult<List<ModelFormFieldVO>> getFormFieldList(Long id);

    /**
     * 列表
     *
     * @return 返回产品线列表
     */
    BaseResult<List<ModelVO>> modelList();

    /**
     * 列表
     *
     * @return 返回产品线列表
     */
    BaseResult<PageQueryResult<ModelVO>> modelList(ModelQueryList modelQueryList);
    /**
     * 新增
     *
     * @param modelAddReq 模块新增请求
     * @return 数量
     */
    BaseResult<Boolean> add(ModelAddReq modelAddReq);

    /**
     * 新增
     *
     * @param modelModifyReq 模块新增请求
     * @return 数量
     */
    BaseResult<Boolean> update(ModelModifyReq modelModifyReq);
}

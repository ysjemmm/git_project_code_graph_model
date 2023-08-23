package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.UpdateProductLineListingStatusReq;
import com.timevale.forward.facade.api.query.ProductLineQueryList;
import com.timevale.forward.facade.api.request.ProductLineAddReq;
import com.timevale.forward.facade.api.request.ProductLineModifyReq;
import com.timevale.forward.facade.api.result.ProductLineModelVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/13 17:02
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProductLineService {

    /**
     * 列表
     *
     * @return 返回产品线列表
     */
    BaseResult<List<ProductLineVO>> productLineList();

    /**
     * 列表
     *
     * @return 返回产品线列表
     */
    BaseResult<PageQueryResult<ProductLineVO>> productLineList(ProductLineQueryList productLineQueryList);

    /**
     * 列表
     *
     * @return 返回产品线列表
     */
    BaseResult<List<ProductLineVO>> getProductLines(Long projectId);

    /**
     * 列表含模块信息
     *
     * @return 返回产品线列表
     */
    BaseResult<List<ProductLineModelVO>> listProductLineModes();

    /**
     * 新增
     *
     * @param productLineAddReq 产品线新增请求
     * @return 数量
     */
    BaseResult<Boolean> add(ProductLineAddReq productLineAddReq);

    /**
     * 修改
     *
     * @param productLineModifyReq 产品线新增请求
     * @return 数量
     */
    BaseResult<Boolean> update(ProductLineModifyReq productLineModifyReq);

    /**
     * 修改产品线上架状态
     *
     * @param req 请求对象
     */
    BaseResult<Boolean> updateProductLineListingStatus(UpdateProductLineListingStatusReq req);

    /**
     * 删除产品线
     *
     * @param productLineId 产品线ID
     */
    BaseResult<Boolean> deleteProductLine(Long productLineId);

}

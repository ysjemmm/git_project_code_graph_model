package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProductDemandService {
    /**
     * 查列表
     *
     * @param productDemandQueryList 产品需求信息
     * @return 列表
     */
    BaseResult<PageQueryResult<ProductDemandVO>> list(ProductDemandQueryList productDemandQueryList);

    /**
     * 修改状态
     *
     * @param productDemandId 产品需求id
     * @param type            操作类型 暂停,启用
     * @return 数量
     */
    BaseResult<Boolean> updateStatus(Long productDemandId, Byte type);

    /**
     * 新增
     *
     * @param productDemandAddReq 产品需求信息
     * @return 数量
     */
    BaseResult<Boolean> add(ProductDemandAddReq productDemandAddReq);


    /**
     * 修改
     *
     * @param productDemandModifyReq 产品需求信息
     * @return 数量
     */
    BaseResult<Boolean> modify(ProductDemandModifyReq productDemandModifyReq);

    /**
     * 查看
     *
     * @param productDemandId 产品需求id
     * @return 详情信息
     */
    BaseResult<ProductDemandDetailVO> get(Long productDemandId);


    /**
     * 查询满足条件的产品需求列表
     *
     * @param productDemandId 产品需求id
     * @return 列表
     */
    BaseResult<PageQueryResult<ProjectVO>> matchProjectList(Long productDemandId);

    /**
     * 查询满足条件的业务需求列表
     *
     * @param productDemandId 产品需求id
     * @return 列表
     */
    BaseResult<PageQueryResult<BizDemandVO>> matchBizDemandList(Long productDemandId);


}

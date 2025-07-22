package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProductDemandGroupQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ProductDemandGroupVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * 产品需求分组服务接口
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProductDemandGroupService {

    /**
     * 查询待排序的产品需求
     *
     * @param productDemandGroupQueryList 产品分组查询条件信息
     * @return 列表
     */
    BaseResult<PageQueryResult<ProductDemandVO>> listProductDemandBacklog(ProductDemandGroupQueryList productDemandGroupQueryList);


    /**
     * 查询业务域需求分组列表
     * @param productDemandGroupQueryList 产品需求查询信息
     * @return 产品需求分组VO列表
     */
    BaseResult<PageQueryResult<ProductDemandGroupVO>> listProductDemandGroup(ProductDemandGroupQueryList productDemandGroupQueryList);

    /**
     * 根据id获取产品需求分组
     * @param id 主键id
     * @return 产品需求分组VO
     */
    BaseResult<ProductDemandGroupVO> getProductDemandGroupById(Long id);

    /**
     * 新增产品需求分组
     * @param productDemandGroupAddReq 新增请求
     * @return 是否成功
     */
    BaseResult<Boolean> add(ProductDemandGroupAddReq productDemandGroupAddReq);

    /**
     * 修改产品需求分组
     * @param productDemandGroupModifyReq 产品需求分组修改请求
     * @return 是否成功
     */
    BaseResult<Boolean> modify(ProductDemandGroupModifyReq productDemandGroupModifyReq);

    /**
     * 删除产品需求分组
     * @param productDemandGroupReq 产品需求分组请求
     * @return 是否成功
     */
    BaseResult<Boolean> delete(ProductDemandGroupReq productDemandGroupReq);


    /**
     * 绑定产品需求分组
     * @param productDemandGroupProjectReq 产品需求分组项目请求
     * @return 是否成功
     */
    BaseResult<Boolean> bindProject(ProductDemandGroupProjectReq productDemandGroupProjectReq);

    /**
     * 拖动产品需求
     * @param productDemandGroupMoveReq 产品需求分组拖动请求
     * @return 是否成功
     */
    BaseResult<Boolean> moveProductDemandGroup(ProductDemandGroupMoveReq productDemandGroupMoveReq);

    /**
     * 拖动产品需分组
     * @param productDemandGroupItemMoveReq 产品需求拖动请求
     * @return 是否成功
     */
    BaseResult<Boolean> moveProductDemand(ProductDemandGroupItemMoveReq productDemandGroupItemMoveReq);
} 
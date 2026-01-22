package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProductDemandGroupQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ProductDemandGroupItemVO;
import com.timevale.forward.facade.api.result.ProductDemandGroupResourcePlanVO;
import com.timevale.forward.facade.api.result.ProductDemandGroupVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
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
     * @return 列表（productDemandGroupId固定为-1，表示待规划）
     */
    BaseResult<PageQueryResult<ProductDemandGroupItemVO>> listProductDemandBacklog(ProductDemandGroupQueryList productDemandGroupQueryList);


    /**
     * 查询业务域集需求分组列表
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
     * 拖动产品分组
     * @param productDemandGroupMoveReq 产品需求分组拖动请求
     * @return 是否成功
     */
    BaseResult<Boolean> moveProductDemandGroup(ProductDemandGroupMoveReq productDemandGroupMoveReq);

    /**
     * 拖动产品需求
     * @param productDemandGroupItemMoveReq 产品需求拖动请求
     * @return 是否成功
     */
    BaseResult<Boolean> moveProductDemand(ProductDemandGroupItemMoveReq productDemandGroupItemMoveReq);

    /**
     * 关联or取消关联项目
     * @param productDemandGroupItemMoveReq 关联项目请求
     * @return 是否成功
     */
    BaseResult<Boolean> linkOrUnlinkProject(ProductDemandGroupProjectLinkReq productDemandGroupItemMoveReq);

    /**
     * 转交产品分组
     * @param productDemandGroupTransferReq 产品需求分组转交请求
     * @return 是否成功
     */
    BaseResult<Boolean> transferProductDemandGroup(ProductDemandGroupTransferReq productDemandGroupTransferReq);

    /**
     * 需求分组内资源规划
     * @param productDemandGroupResourcePlanReq 产品需求分组资源规划请求
     * @return 是否成功
     */
    BaseResult<Boolean> upsertResourcePlan(ProductDemandGroupResourcePlanReq productDemandGroupResourcePlanReq, boolean isAloneUpdate, boolean isUpdateTime);


    BaseResult<Boolean> addDemand(ProductDemandGroupInnerAddReq productDemandGroupInnerAddReq);

    /**
     * 需求分组内资源规划信息
     * @param productDemandGroupId 产品需求分组id
     * @return ProductDemandGroupResourcePlanVO
     */
    BaseResult<ProductDemandGroupResourcePlanVO> getResourcePlan(Long bizDomainGroupId, Long productDemandGroupId);
}

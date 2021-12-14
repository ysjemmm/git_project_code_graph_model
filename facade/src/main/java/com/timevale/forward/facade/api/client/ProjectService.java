package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectService {
    /**
     * 查列表
     *
     * @param projectQueryList 项目信息
     * @return 列表
     */
    BaseResult<PageQueryResult<ProjectVO>> list(ProjectQueryList projectQueryList);

    /**
     * 修改状态
     *
     * @param id   项目id
     * @param type 操作类型 暂停,作废
     * @return 数量
     */
    BaseResult<Integer> updateStatus(Long id, Byte type);

    /**
     * 开启项目
     *
     * @param id 项目id
     * @return 数量
     */
    BaseResult<Integer> enable(Long id);

    /**
     * 新增
     *
     * @param projectAddReq 项目信息
     * @return 数量
     */
    BaseResult<Integer> add(ProjectAddReq projectAddReq);


    /**
     * 修改
     *
     * @param projectModifyReq 项目信息
     * @return 数量
     */
    BaseResult<Integer> modify(ProjectModifyReq projectModifyReq);

    /**
     * 查看
     *
     * @param id 项目信息
     * @return 详情信息
     */
    BaseResult<ProjectDetailVO> get(Long id);


    /**
     * 查询满足条件的产品需求列表
     *
     * @param id 项目信息
     * @return 列表
     */
    BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(Long id);


    /**
     *关联or取消关联
     * @param productDemandId 产品需求id
     * @param type 关联or取消关联
     * @return 数量
     */
    BaseResult<Integer> linkOrUnLinkProductDemand(Long id,Long productDemandId, Byte type);

}

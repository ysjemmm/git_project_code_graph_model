package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

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
    BaseResult<QueryResultVO<ProjectVO>> list(ProjectQueryList projectQueryList);

    /**
     * 修改状态
     *
     * @param req 暂停/作废更新
     * @return 数量
     */
    BaseResult<Boolean> updateStatus(ProjectUpdateStatusReq req);

    /**
     * 开启项目
     *
     * @param projectId 项目id
     * @param enableTask 是否启用任务
     * @return 数量
     */
    BaseResult<Boolean> enable(Long projectId,Boolean enableTask);

    /**
     * 新增
     *
     * @param projectAddReq 项目信息
     * @return 数量
     */
    BaseResult<Boolean> add(ProjectAddReq projectAddReq);


    /**
     * 修改
     *
     * @param projectModifyReq 项目信息
     * @return 数量
     */
    BaseResult<Boolean> modify(ProjectModifyReq projectModifyReq);

    /**
     * 查看
     *
     * @param projectId 项目信息
     * @return 详情信息
     */
    BaseResult<ProjectDetailVO> get(Long projectId);


    /**
     * 查询满足条件的产品需求列表
     *
     * @param query 项目信息
     * @return 列表
     */
    BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(ProjectLinkProductDemandQueryList query);


    /**
     * 关联or取消关联
     *
     * @param productDemandLinkReq 产品需求
     */
    BaseResult<ProductDemandStatusVO> linkOrUnLinkProductDemand(ProjectProductDemandLinkReq productDemandLinkReq);


    /**
     *
     * @param query 查询条件
     * @return 项目产品需求清单
     */
    BaseResult<PageQueryResult<ProductDemandVO>>  linkProductDemandList(ProjectProductDemandQueryList query);

    /**
     * 产品线Id
     *
     * @param productLineId 产品id
     * @return 项目简单VO列表
     */
    BaseResult<List<ProjectBaseVO>> getProjectByProductLine(Long productLineId);

    /**
     * 修改立项日期
     *
     * @param projectDateModifyReq 修改立项日期
     * @return Boolean
     */
    BaseResult<Boolean> modifyProjectDate(ProjectDateModifyReq projectDateModifyReq);
}

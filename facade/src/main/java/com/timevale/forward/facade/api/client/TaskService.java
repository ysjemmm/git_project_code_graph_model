package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.TaskLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.request.TaskModifyReq;
import com.timevale.forward.facade.api.request.TaskProductDemandLinkReq;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.TaskDetailVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface TaskService {


    /**
     * 查列表
     *
     * @param taskQueryList 项目信息
     * @return 列表
     */
    BaseResult<PageQueryResult<TaskVO>> list(TaskQueryList taskQueryList);

    /**
     * 修改
     *
     * @param taskAddReq 任务信息
     * @return 数量
     */
    BaseResult<Boolean> add(TaskAddReq taskAddReq);


    /**
     * 修改
     *
     * @param taskModifyReq 任务信息
     * @return 数量
     */
    BaseResult<Boolean> modify(TaskModifyReq taskModifyReq);

    /**
     * 查看
     *
     * @param taskId 任务信息
     * @return 详情信息
     */
    BaseResult<TaskDetailVO> get(Long taskId);

    /**
     * 修改状态
     *
     * @param taskId, 任务id
     * @param type    操作类型 暂停,作废
     * @return 数量
     */
    BaseResult<Boolean> updateStatus(Long taskId, Integer type);

    /**
     * 开启项目
     *
     * @param taskId 任务id
     * @return 数量
     */
    BaseResult<Boolean> enable(Long taskId);

    /**
     * 查询满足条件的产品需求列表
     *
     * @param taskLinkProductDemandQueryList 项目信息
     * @return 列表
     */
    BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(TaskLinkProductDemandQueryList taskLinkProductDemandQueryList);


    /**
     * 关联or取消关联
     *
     * @param taskProductDemandLinkReq 产品需求
     */
    BaseResult<Boolean> linkOrUnLinkProductDemand(TaskProductDemandLinkReq taskProductDemandLinkReq);

    /**
     * @param taskProductDemandQueryList 查询条件
     * @return 任务产品需求清单
     */
    BaseResult<PageQueryResult<ProductDemandVO>> linkProductDemandList(TaskProductDemandQueryList taskProductDemandQueryList);


}

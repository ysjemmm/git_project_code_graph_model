package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProductDemandLinkTaskQueryList;
import com.timevale.forward.facade.api.query.TaskLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.TaskDetailVO;
import com.timevale.forward.facade.api.result.TaskListVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.math.BigDecimal;
import java.util.List;

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
    BaseResult<Long> add(TaskAddReq taskAddReq);


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
     * 执行项目
     *
     * @param executeReq 任务执行请求
     * @return 是否成功
     */
    BaseResult<Boolean> execute(TaskExecuteReq executeReq);

    /**
     * 完成项目
     *
     * @param doneReq 任务完成请求
     * @return 是否成功
     */
    BaseResult<Boolean> done(TaskDoneReq doneReq);

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

    /**
     *
     * @param elapsedTimeQueryReq 查询条件
     * @return 耗时(h)
     */
    BaseResult<BigDecimal> getElapsedTime(ElapsedTimeQueryReq elapsedTimeQueryReq);

    /**
     * @param productDemandLinkTaskQueryList 查询条件
     * @return 任务需求清单
     */
    BaseResult<PageQueryResult<TaskListVO>> listTask(ProductDemandLinkTaskQueryList productDemandLinkTaskQueryList);

    /**
     * 修改
     *
     * @param taskBatchAddReq 任务信息
     * @return 数量
     */
    BaseResult<Boolean> batchAdd(TaskBatchAddReq taskBatchAddReq);

    /**
     *
     * @param elapsedEndTimeQueryReq 查询条件
     * @return yyyy-MM-dd HH:mm:ss
     */
    BaseResult<String> getElapsedEndTime(ElapsedEndTimeQueryReq elapsedEndTimeQueryReq);


    /**
     *  无16h限制的产品线
     * @return 产品线id
     */
    BaseResult<List<Long>> getProductLineIdsUnLimited();

    /**
     * 转移任务到其他项目
     *
     * @param transferReq transferReq
     * @return Boolean
     */
    BaseResult<Boolean> transferTask(TaskTransferReq transferReq);
}

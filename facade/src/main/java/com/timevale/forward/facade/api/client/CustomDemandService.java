package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.CustomDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author by YangXu
 * @date 2021/12/14 14:03
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface CustomDemandService {

    /**
     * 列表
     *
     * @param customDemandQueryList 客户需求查询列表
     * @return 列表
     */
    BaseResult<PageQueryResult<CustomDemandVO>> list(CustomDemandQueryList customDemandQueryList);


    /**
     * 新增客户需求
     *
     * @param customDemandAddReq 新增客户需求
     * @return 成功与否
     */
    BaseResult<Boolean> add(CustomDemandAddReq customDemandAddReq);

    /**
     * 通过id获取客户需求
     *
     * @param customDemandId 客户需求id
     * @return 单个客户需求详情
     */
    BaseResult<CustomDemandVO> get(Long customDemandId);


    /**
     * 同意接收
     *
     * @param customDemandId 客户需求同意要求的事情
     * @return 成功与否
     */
    BaseResult<Boolean> agree(Long customDemandId);

    /**
     * 驳回
     *
     * @param customDemandRejectReqc 客户需求驳回要求的事情
     * @return 成功与否
     */
    BaseResult<Boolean> reject(CustomDemandRejectReq customDemandRejectReqc);

//    /**
//     * 批量转交
//     *
//     * @param batchTransferReq 客户需求批量转交请求 - 接收人
//     * @return 成功与否
//     */
//    BaseResult<Boolean> transfer(BatchTransferReq batchTransferReq);

    /**
     * 已处理（无需开发）
     *
     * @param customDemandCompletedReq 客户需求完成
     */
    BaseResult<Boolean> completed(CustomDemandCompletedReq customDemandCompletedReq);


}

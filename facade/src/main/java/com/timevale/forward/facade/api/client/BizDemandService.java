package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author by YangXu
 * @date 2021/12/14 14:03
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BizDemandService {

    /**
     * 列表
     *
     * @param bizDemandQueryList 业务需求查询列表
     * @return 列表
     */
    BaseResult<PageQueryResult<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList);

    /**
     * 作废
     *
     * @param bizDemandUpdateStatusReq 业务需求更新状态要求的事情
     * @return 成功与否
     */
    BaseResult<Boolean> updateStatus(BizDemandUpdateStatusReq bizDemandUpdateStatusReq);

    /**
     * 新增业务需求
     *
     * @param bizDemandAddReq 业务需求添加请求
     * @return 成功与否
     */
    BaseResult<Boolean> add(BizDemandAddReq bizDemandAddReq);

    /**
     * 通过id获取业务需求
     *
     * @param bizDemandId 业务需求id
     * @return 单个业务需求详情
     */
    BaseResult<BizDemandDetailVO> getBizDemandById(Long bizDemandId);

    /**
     * 修改业务需求
     *
     * @param bizDemandModifyReq 业务需求修改请求
     * @return 成功与否
     */
    BaseResult<Boolean> modify(BizDemandModifyReq bizDemandModifyReq);

    /**
     * 同意接收
     *
     * @param bizDemandAgreeReq 业务需求同意要求的事情
     * @return 成功与否
     */
    BaseResult<Boolean> agree(BizDemandAgreeReq bizDemandAgreeReq);

    /**
     * 驳回
     *
     * @param bizDemandRejectReq 业务需求拒绝要求的事情
     * @return 成功与否
     */
    BaseResult<Boolean> reject(BizDemandRejectReq bizDemandRejectReq);

    /**
     * 转移
     *
     * @param bizDemandTransferReq 业务需求转交请求
     * @return 成功与否
     */
    BaseResult<Boolean> transfer(BizDemandTransferReq bizDemandTransferReq);
}

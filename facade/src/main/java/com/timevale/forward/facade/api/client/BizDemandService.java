package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

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
    BaseResult<QueryResultVO<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList);

    /**
     * 列表分类 by 产品线
     *
     * @param bizDemandQueryList 业务需求查询列表
     * @return {@link BaseResult}<{@link List}<{@link ProductLineAnalyseVO}>>
     */
    BaseResult<List<ProductLineAnalyseVO>> listClassify(BizDemandQueryList bizDemandQueryList);

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
     * 转交
     *
     * @param bizDemandTransferReq 业务需求转交请求
     * @return 成功与否
     */
    BaseResult<Boolean> transfer(BizDemandTransferReq bizDemandTransferReq);

    /**
     * 批量转交
     *
     * @param batchTransferReq 业务需求批量转交请求 - 接收人
     * @return 成功与否
     */
    BaseResult<Boolean> bizDemandBatchTransferReceiveMan(BatchTransferReq batchTransferReq);

    /**
     * 批量转交
     *
     * @param batchTransferReq 业务需求批量转交请求 - 提交人
     * @return 成功与否
     */
    BaseResult<Boolean> bizDemandBatchTransferCreateMan(BatchTransferReq batchTransferReq);

    /**
     * 已处理（无需开发）
     *
     * @param bizDemandCompleted 业务需求完成
     */
    BaseResult<Boolean> completed(BizDemandCompletedReq bizDemandCompleted);

    /**
     * 已处理（无需开发）同意
     *
     * @param bizDemandCompletedAgreeReq 业务需求完成同意要求事情
     */
    BaseResult<Boolean> completedAgree(BizDemandCompletedAgreeReq bizDemandCompletedAgreeReq);

    /**
     * 已处理（无需开发）拒绝
     *
     * @param bizDemandCompletedRejectReq 业务需求完成拒绝要求事情
     */
    BaseResult<Boolean> completedReject(BizDemandCompletedRejectReq bizDemandCompletedRejectReq);

    /**
     * 被驳回后 可重新提交
     * @param bizDemandResubmitReq bizDemandResubmitReq
     * @return Boolean
     */
    BaseResult<Boolean> reSubmit(BizDemandResubmitReq bizDemandResubmitReq);

    /**
     * 通过id获取业务需求,客开项目用
     *
     * @param bizDemandGetReq 业务需求id
     * @return 多个业务需求详情
     */
    BaseResult<List<BizDemandSimpleVO>> getSimpleBizDemands(BizDemandGetReq bizDemandGetReq);


    /**
     * 根据客户id查询业务数据
     */
    BaseResult<List<BizDemandVO>> getBizDemandByCustomId(Long customId);

    /**
     * CRM开发资源申请流程通过,发送消息通知接收人
     */
    BaseResult<Boolean> noticeReceiver(BizDemandNoticeReceiverReq receiverReq);

    /**
     * 业务需求-项目清单
     *
     * @param bizDemandId 业务需求id
     * @return 列表
     */
    ProjectVO findLinkProject(Long bizDemandId);

}

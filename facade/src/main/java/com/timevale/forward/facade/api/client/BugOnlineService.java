package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BugOnlineDetailVO;
import com.timevale.forward.facade.api.result.BugOnlineSimpleVO;
import com.timevale.forward.facade.api.result.BugOnlineVO;
import com.timevale.forward.facade.api.result.ProductLineToFieldVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @Date 2022/3/17 10:22
 * @Author 望轩
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BugOnlineService {
    /**
     * 从配置中心获取产品线id应该显示的字段
     *
     * @param bugOnlineGetFieldReq 参数
     * @return 返回值
     */
    BusinessResult<ProductLineToFieldVO> getAllDisplayField(BugOnlineGetFieldReq bugOnlineGetFieldReq);

    /**
     * 查询线上bug列表
     *
     * @param bugOnlineQueryList 查询参数
     * @return 返回值
     */
    BaseResult<PageQueryResult<BugOnlineVO>> list(BugOnlineQueryList bugOnlineQueryList);

    /**
     * 新增线上bug
     *
     * @param addReq 新增参数
     * @return 返回值
     */
    BaseResult<Boolean> add(BugOnlineAddReq addReq);

    /**
     * 删除线上bug
     *
     * @param deleteReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> delete(BugOnlineReq deleteReq);

    /**
     * 编辑线上bug
     *
     * @param modifyReq 参数
     * @return 返回值
     */
    BaseResult<String> modify(BugOnlineModifyReq modifyReq);

    /**
     * 查看线上bug详情
     *
     * @param getReq 参数
     * @return 返回值
     */
    BusinessResult<BugOnlineDetailVO> get(BugOnlineDetailReq getReq);

    /**
     * bug确认
     *
     * @param bugOnlineReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> confirm(BugOnlineReq bugOnlineReq);

    /**
     * 开始修复
     *
     * @param startRepairReq 参数
     * @return 返回值
     */
    BusinessResult<String> startRepair(BugOnlineStartRepairReq startRepairReq);

    /**
     * 修复完毕
     *
     * @param bugOnlineRepairFinishedReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> repairFinished(BugOnlineRepairFinishedReq bugOnlineRepairFinishedReq);

    /**
     * 确认修复
     *
     * @param confirmRepairReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> confirmRepair(BugOnlineConfirmRepairReq confirmRepairReq);

    /**
     * 已上线
     *
     * @param onlineReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> online(BugOnlineOnlineReq onlineReq);

    /**
     * 重新打开
     *
     * @param bugOnlineOpenAgainReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> openAgain(BugOnlineOpenAgainReq bugOnlineOpenAgainReq);

    /**
     * 不用修复
     *
     * @param noRepairReq 参数
     * @return 返回值
     */
    BusinessResult<String> noRepair(BugOnlineNoRepairReq noRepairReq);

    /**
     * 转交
     *
     * @param bugOnlineTransferReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> transfer(BugOnlineTransferReq bugOnlineTransferReq);

    /**
     * 同意
     *
     * @param bugOnlineReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> agree(BugOnlineReq bugOnlineReq);

    /**
     * 拒绝
     *
     * @param bugOnlineReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> reject(BugOnlineReq bugOnlineReq);

    /**
     * 重新确认
     *
     * @param bugOnlineReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> reconfirm(BugOnlineReq bugOnlineReq);

    /**
     * 暂不修复
     *
     * @param bugOnlineReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> temporaryNoRepair(BugOnlineReq bugOnlineReq);

    /**
     * 修复失败
     *
     * @param bugOnlineRepairFailedReasonReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> repairFailed(BugOnlineRepairFailedReasonReq bugOnlineRepairFailedReasonReq);

    /**
     * 查询线上bug列表
     *
     * @param bugOnlineGetReq 查询参数
     * @return 返回值
     */
    BaseResult<List<BugOnlineVO>> getByName(BugOnlineGetReq bugOnlineGetReq);

    /**
     * 根据客户id查询业务数据
     * @param customId
     * @return
     */
    BaseResult<List<BugOnlineVO>> getBugOnlineByCustomId(Long customId);

    /**
     * 批量查询指定id的线上bug数据
     *
     * @param simpleReq 线上bug id集合
     * @return 线上bug数据
     */
    BaseResult<List<BugOnlineSimpleVO>> getByIds(BugOnlineIdsReq simpleReq);


    /**
     * bug 关联需求列表转需求接口
     */
    BaseResult<Void> attachToBizDemand(BugOnlineAttachToBizReq attachToBizReq);

    /**
     * 线上bug转业务需求操作
     *
     * @param toBizApplyReq 操作请求
     */
    BaseResult<Void> convertBizApply(BugOnlineToBizApplyReq toBizApplyReq);

    /**
     * 开始响应
     *
     * @param id 线上bug id
     */
    BaseResult<Void> startResponse(Long id);

    /**
     * 验收
     *
     * @param acceptanceReq 验收请求
     */
    BaseResult<Void> acceptance(BugOnlineAcceptanceReq acceptanceReq);

    /**
     * 得到优先级
     *
     * @param getPriorityReq 得到优先级请求
     * @return {@link BaseResult}<{@link Integer}>
     */
    BaseResult<Integer> getPriority(BugOnlinePriorityGetReq getPriorityReq);
}

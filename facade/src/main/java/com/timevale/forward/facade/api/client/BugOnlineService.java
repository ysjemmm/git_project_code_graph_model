package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BugOnlineDetailVO;
import com.timevale.forward.facade.api.result.BugOnlineVO;
import com.timevale.forward.facade.api.result.ProductLineToFieldVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;

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
     * @param bugOnlineAddReq 新增参数
     * @return 返回值
     */
    BusinessResult<Boolean> add(BugOnlineAddReq bugOnlineAddReq);

    /**
     * 删除线上bug
     *
     * @param bugOnlineReq 参数
     * @return 返回值
     * */
    BusinessResult<Boolean> delete(BugOnlineReq bugOnlineReq);

    /**
     * 编辑线上bug
     *
     * @param bugOnlineModifyReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> modify(BugOnlineModifyReq bugOnlineModifyReq);

    /**
     * 查看线上bug详情
     *
     * @param bugOnlineDetailReq 参数
     * @return 返回值
     */
    BusinessResult<BugOnlineDetailVO> get(BugOnlineDetailReq bugOnlineDetailReq);

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
     * @param bugOnlineStartRepairReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> startRepair(BugOnlineStartRepairReq bugOnlineStartRepairReq);

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
     * @param bugOnlineConfirmRepairReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> confirmRepair(BugOnlineConfirmRepairReq bugOnlineConfirmRepairReq);

    /**
     * 已上线
     *
     * @param bugOnlineOnlineReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> online(BugOnlineOnlineReq bugOnlineOnlineReq);

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
     * @param bugOnlineNoRepairReq 参数
     * @return 返回值
     */
    BusinessResult<Boolean> noRepair(BugOnlineNoRepairReq bugOnlineNoRepairReq);

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
}

































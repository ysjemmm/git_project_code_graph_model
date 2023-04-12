package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BugLogQueryList;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BugLogVO;
import com.timevale.forward.facade.api.result.BugOfflineDetailVO;
import com.timevale.forward.facade.api.result.BugOfflineVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BugOfflineService {


    /**
     * 查列表
     *
     * @param bugOfflineQueryList bug信息
     * @return 列表
     */
    BaseResult<PageQueryResult<BugOfflineVO>> list(BugOfflineQueryList bugOfflineQueryList);

    /**
     * 修改
     *
     * @param bugOfflineAddReq bug信息
     * @return Long
     */
    BaseResult<Long> add(BugOfflineAddReq bugOfflineAddReq);


    /**
     * 修改
     *
     * @param bugOfflineModifyReq bug信息
     * @return Boolean
     */
    BaseResult<Boolean> modify(BugOfflineModifyReq bugOfflineModifyReq);

    /**
     * 转交
     *
     * @param bugOfflineTransferReq bugOfflineTransferReq
     * @return Boolean
     */
    BaseResult<Boolean> transfer(BugOfflineTransferReq bugOfflineTransferReq);

    /**
     * 不用修复
     *
     * @param bugOfflineUnHandleReq 不用修复参数
     * @return Boolean
     */
    BaseResult<Boolean> unHandle(BugOfflineUnHandleReq bugOfflineUnHandleReq);

    /**
     * 同意
     *
     * @param bugOfflineReq 参数
     * @return Boolean
     */
    BaseResult<Boolean> agree(BugOfflineReq bugOfflineReq);

    /**
     * 拒绝
     *
     * @param bugOfflineReq 参数
     * @return Boolean
     */
    BaseResult<Boolean> reject(BugOfflineReq bugOfflineReq);

    /**
     * 延期修复
     *
     * @param bugOfflineDelayHandleReq 延期修复参数
     * @return Boolean
     */
    BaseResult<Boolean> delayHandle(BugOfflineDelayHandleReq bugOfflineDelayHandleReq);

    /**
     * 确认修复
     *
     * @param bugOfflineReq 参数
     * @return Boolean
     */
    BaseResult<Boolean> doHandle(BugOfflineReq bugOfflineReq);

    /**
     * 自测通过
     *
     * @param bugOfflineReq 参数
     * @return Boolean
     */
    BaseResult<Boolean> passSelf(BugOfflinePassSelfReq bugOfflineReq);

    /**
     * 验收通过
     *
     * @param bugOfflineReq 参数
     * @return Boolean
     */
    BaseResult<Boolean> accepted(BugOfflineReq bugOfflineReq);

    /**
     * 验收失败
     *
     * @param bugOfflineReq 参数
     * @return Boolean
     */
    BaseResult<Boolean> acceptFailed(BugOfflineReq bugOfflineReq);

    /**
     * 重新打开
     *
     * @param bugOfflineReq 参数
     * @return Boolean
     */
    BaseResult<Boolean> reopen(BugOfflineReq bugOfflineReq);

    /**
     * 查看
     *
     * @param id bug id
     * @return 详情信息
     */
    BaseResult<BugOfflineDetailVO> get(Long id);

    /**
     * 删除
     *
     * @param bugOfflineReq 参数
     * @return Boolean
     */
    BaseResult<Boolean> delete(BugOfflineReq bugOfflineReq);

    /**
     * 查询bug日志列表
     *
     * @param logQuery 参数
     * @return 返回分页结果
     */
    BaseResult<PageQueryResult<BugLogVO>> bugLogList(BugLogQueryList logQuery);


}

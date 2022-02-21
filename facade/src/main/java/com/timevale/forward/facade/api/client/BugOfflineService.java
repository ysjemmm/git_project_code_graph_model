package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.BugOfflineAddReq;
import com.timevale.forward.facade.api.request.BugOfflineModifyReq;
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
     * @return Boolean
     */
    BaseResult<Boolean> add(BugOfflineAddReq bugOfflineAddReq);


    /**
     * 修改
     *
     * @param bugOfflineModifyReq bug信息
     * @return Boolean
     */
    BaseResult<Boolean> modify(BugOfflineModifyReq bugOfflineModifyReq);

    /**
     * 不用修复
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> unHandle(Long id);

    /**
     * 同意
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> agree(Long id);

    /**
     * 拒绝
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> reject(Long id);

    /**
     * 延期修复
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> delayHandle(Long id);

    /**
     * 确认修复
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> doHandle(Long id);

    /**
     * 自测通过
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> passSelf(Long id);

    /**
     * 验收通过
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> accepted(Long id);

    /**
     * 验收失败
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> acceptFailed(Long id);

    /**
     * 重新打开
     *
     * @param id  bug id
     * @return Boolean
     */
    BaseResult<Boolean> reopen(Long id);

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
     * @param id bug id
     * @return Boolean
     */
    BaseResult<Boolean> delete(Long id);


}

package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizChangeLogQueryList;
import com.timevale.forward.facade.api.result.BizChangeLogVO;
import com.timevale.forward.facade.api.result.BizRecordVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;


/**
 * 商业变化日志服务
 *
 * @author yangxu
 * @date 2022/04/12
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BizChangeLogService {

    /**
     * 列表
     *
     * @param bizChangeLogQueryList 业务变更记录查询列表
     * @return 列表
     */
    BaseResult<PageQueryResult<BizChangeLogVO>> list(BizChangeLogQueryList bizChangeLogQueryList);

    /**
     * 查询状态操作人员日志
     *
     * @param query 查询
     * @return 状态操作人员列表
     */
    BaseResult<PageQueryResult<BizRecordVO>> statusOperator(BizChangeLogQueryList query);
}

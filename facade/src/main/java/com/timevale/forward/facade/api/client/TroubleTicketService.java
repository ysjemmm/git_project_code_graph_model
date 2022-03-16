package com.timevale.forward.facade.api.client;

import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.TroubleTicketQueryList;
import com.timevale.forward.facade.api.request.TroubleTicketAddReq;
import com.timevale.forward.facade.api.request.TroubleTicketDeleteReq;
import com.timevale.forward.facade.api.request.TroubleTicketModifyReq;
import com.timevale.forward.facade.api.result.TroubleTicketDetailVO;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.BusinessResult;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/03/16 17:46
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface TroubleTicketService {

    /**
     * 新增故障单
     *
     * @param troubleTicketAddReq 故障单添加请求
     * @return 成功与否
     */
    BusinessResult<Boolean> add(TroubleTicketAddReq troubleTicketAddReq);

    /**
     * 修改故障单
     *
     * @param troubleTicketModifyReq 故障单修改请求
     * @return 成功与否
     */
    BusinessResult<Boolean> modify(TroubleTicketModifyReq troubleTicketModifyReq);

    /**
     * 查看故障单
     *
     * @param troubleTicketId 故障单id
     * @return 故障单详情
     */
    BusinessResult<TroubleTicketDetailVO> get(Long troubleTicketId);

    /**
     * 删除故障单
     *
     * @param troubleTicketDeleteReq 故障单删除请求
     * @return 成功与否
     */
    BusinessResult<Boolean> delete( TroubleTicketDeleteReq troubleTicketDeleteReq);

    /**
     * 故障单列表查询
     *
     * @param troubleTicketQueryList 故障单列表查询条件
     * @return 故障单列表
     */
    BusinessResult<List<TroubleTicketVO>> list( TroubleTicketQueryList troubleTicketQueryList);

}

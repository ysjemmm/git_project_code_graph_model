package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.TroubleTicketCondition;
import com.timevale.forward.dal.entity.TroubleTicketDO;
import com.timevale.forward.dal.entity.TroubleTicketListDO;
import com.timevale.forward.facade.api.query.TroubleTicketQueryList;
import com.timevale.forward.facade.api.request.TroubleTicketAddReq;
import com.timevale.forward.facade.api.request.TroubleTicketModifyReq;
import com.timevale.forward.facade.api.result.TroubleTicketDetailVO;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2022/03/17 11:24
 */
@Mapper
public interface TroubleTicketCopier {
    TroubleTicketCopier INSTANCE = Mappers.getMapper(TroubleTicketCopier.class);

    /**
     * 转换
     *
     * @param troubleTicketAddReq 故障单-添加请求
     * @return 故障单 DO
     */
    TroubleTicketDO convert(TroubleTicketAddReq troubleTicketAddReq);

    /**
     * 转换
     *
     * @param troubleTicketModifyReq 故障单-修改请求
     * @return 故障单 DO
     */
    TroubleTicketDO convert(TroubleTicketModifyReq troubleTicketModifyReq);


    /**
     * 转换
     *
     * @param troubleTicketDO 故障单-DO
     * @return 故障单详细信息
     */
    TroubleTicketDetailVO convert(TroubleTicketDO troubleTicketDO);

    /**
     * 转换
     *
     * @param troubleTicketListDO 故障单-列表DO
     * @return 故障单详细信息
     */
    TroubleTicketVO convert(TroubleTicketListDO troubleTicketListDO);

    /**
     * 转换
     *
     * @param troubleTicketQueryList 故障单-列表查询条件
     * @return 故障单条件
     */
    TroubleTicketCondition convert(TroubleTicketQueryList troubleTicketQueryList);

}

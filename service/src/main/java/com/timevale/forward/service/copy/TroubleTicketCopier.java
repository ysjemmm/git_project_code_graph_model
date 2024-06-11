package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.TroubleTicketCondition;
import com.timevale.forward.dal.entity.TroubleTicketDO;
import com.timevale.forward.dal.entity.TroubleTicketListDO;
import com.timevale.forward.facade.api.query.TroubleTicketQueryList;
import com.timevale.forward.facade.api.request.TroubleTicketAddReq;
import com.timevale.forward.facade.api.request.TroubleTicketModifyReq;
import com.timevale.forward.facade.api.result.TroubleTicketDetailVO;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import com.timevale.forward.model.enums.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2022/03/17 11:24
 */
@Mapper(
        imports = {
                TroubleTicketTypeEnum.class,
                TroubleTicketRankEnum.class,
                TroubleTicketCauseEnum.class,
                TroubleTicketReasonEnum.class,
                TroubleTicketDuringTimeEnum.class,
                TroubleTicketCategoryFirstEnum.class,
                TroubleTicketCategorySecondEnum.class,
                TroubleTicketInfluenceScopeEnum.class,
        }
)
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
    @Mapping(target = "typeName", expression = "java(TroubleTicketTypeEnum.getTextByCode(troubleTicketDO.getType()))")
    @Mapping(target = "causeName", expression = "java(TroubleTicketCauseEnum.getTextByCode(troubleTicketDO.getCause()))")
    @Mapping(target = "reasonName", expression = "java(TroubleTicketReasonEnum.getTextByCode(troubleTicketDO.getReason()))")
    @Mapping(target = "troubleRankName", expression = "java(TroubleTicketRankEnum.getTextByCode(troubleTicketDO.getTroubleRank()))")
    @Mapping(target = "duringTimeName", expression = "java(TroubleTicketDuringTimeEnum.getTextByCode(troubleTicketDO.getDuringTime()))")
    @Mapping(target = "categoryFirstName", expression = "java(TroubleTicketCategoryFirstEnum.getTextByCode(troubleTicketDO.getCategoryFirst()))")
    @Mapping(target = "categorySecondName", expression = "java(TroubleTicketCategorySecondEnum.getTextByCode(troubleTicketDO.getCategorySecond()))")
    @Mapping(target = "influenceScopeName", expression = "java(TroubleTicketInfluenceScopeEnum.getTextByCode(troubleTicketDO.getInfluenceScope()))")
    TroubleTicketDetailVO convert(TroubleTicketDO troubleTicketDO);

    /**
     * 转换
     *
     * @param troubleTicketListDO 故障单-列表DO
     * @return 故障单详细信息
     */
    @Mapping(target = "categoryName", expression = "java(TroubleTicketCategorySecondEnum.getFullTextByCode(troubleTicketListDO.getCategorySecond()))")
    TroubleTicketVO convert(TroubleTicketListDO troubleTicketListDO);

    /**
     * 转换
     *
     * @param troubleTicketQueryList 故障单-列表查询条件
     * @return 故障单条件
     */
    TroubleTicketCondition convert(TroubleTicketQueryList troubleTicketQueryList);

}

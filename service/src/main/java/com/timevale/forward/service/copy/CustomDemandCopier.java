package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.CustomDemandListCondition;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.facade.api.query.CustomDemandQueryList;
import com.timevale.forward.facade.api.request.CustomDemandAddReq;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:56
 */
@Mapper
public interface CustomDemandCopier {
    CustomDemandCopier INSTANCE = Mappers.getMapper(CustomDemandCopier.class);

    /**
     * 查询条件转换
     *
     * @param customDemandQueryList 客户需求查询条件
     * @return 查询条件
     */
    CustomDemandListCondition convert(CustomDemandQueryList customDemandQueryList);


    /**
     * 请求添加转换为DO
     *
     * @param customDemandAddReq 客户需求添加
     * @return 客户需求DO
     */
    CustomDemandDO convert(CustomDemandAddReq customDemandAddReq);



    /**
     * 客户需求DO转换为VO
     *
     * @param customDemandDO) 客户需求DO
     * @return 客户需求详细VO
     */
    CustomDemandVO convert(CustomDemandDO customDemandDO);

    /**
     * DO批量转换为VO
     *
     * @param list 列表
     * @return 客户需求列表
     */
    List<CustomDemandVO> convert(List<CustomDemandDO> list);

}

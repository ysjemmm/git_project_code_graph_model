package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @Date 2021/12/15 10:30
 */
@Mapper
public interface ProductLineCopier {

    ProductLineCopier INSTANCE = Mappers.getMapper(ProductLineCopier.class);

    /**
     * 产品线 DO转换VO
     *
     * @param productLineDO 产品线DO
     * @return 产品线VO
     */
    ProductLineVO convert(ProductLineDO productLineDO);

}

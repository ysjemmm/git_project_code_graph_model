package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
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
    @Mapping(source = "owner", target = "productLineOwner")
    @Mapping(source = "ownerId", target = "productLineOwnerId")
    ProductLineVO convert(ProductLineDO productLineDO);

    /**
     * 批量处理
     *
     * @param productLineDOList 产品线do 列表
     * @return 列表
     */
    List<ProductLineVO> convert(List<ProductLineDO> productLineDOList);

}

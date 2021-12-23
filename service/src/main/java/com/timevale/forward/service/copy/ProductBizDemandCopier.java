package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProductBizDemandDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2021/12/16 16:12
 */
@Mapper
public interface ProductBizDemandCopier {
    ProductBizDemandCopier INSTANCE = Mappers.getMapper(ProductBizDemandCopier.class);

    /**
     * 转换
     *
     * @param bizDemandId 业务需求id
     * @param productDemandId   产品id
     * @return DO
     */
    ProductBizDemandDO convert(Long bizDemandId, Long productDemandId);


}

package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProductDemandCopier {

    ProductDemandCopier INSTANCE = Mappers.getMapper(ProductDemandCopier.class);

    /**
     * 转换转换DO
     *
     * @param productDemandAddReq 对象
     * @return ProductDemandDO
     */
    ProductDemandDO convert(ProductDemandAddReq productDemandAddReq);

}

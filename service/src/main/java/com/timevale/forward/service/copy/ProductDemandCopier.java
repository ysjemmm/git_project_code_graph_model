package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.query.ProjectSubProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.result.ProductDemandVO;
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

    /**
     * 转换转换DO
     *
     * @param projectSubProductDemandQueryList 对象
     * @return ProductDemandListCondition
     */
    ProductDemandListCondition convert(ProjectSubProductDemandQueryList projectSubProductDemandQueryList);

    /**
     * 转换转换DO
     *
     * @param productDemandListDO 对象
     * @return ProductDemandVO
     */
    List<ProductDemandVO> convert(List<ProductDemandListDO> productDemandListDO);

}

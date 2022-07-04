package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProductDemandDescFlowDO;
import com.timevale.forward.facade.api.result.ProductDemandDescFlowVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author jingchun
 * create on 2022/7/4
 */
@Mapper
public interface ProductDemandDescFlowCopier {

    ProductDemandDescFlowCopier INSTANCE = Mappers.getMapper(ProductDemandDescFlowCopier.class);

    ProductDemandDescFlowVO convert(ProductDemandDescFlowDO productDemandDescFlowDO);

}

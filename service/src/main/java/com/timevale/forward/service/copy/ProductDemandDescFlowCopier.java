package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProductDemandDescFlowDO;
import com.timevale.forward.facade.api.result.ProductDemandDescFlowVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author jingchun
 * create on 2022/7/4
 */
@Mapper
public interface ProductDemandDescFlowCopier {

    ProductDemandDescFlowCopier INSTANCE = Mappers.getMapper(ProductDemandDescFlowCopier.class);

    @Mapping(target = "taskId", ignore = true)
    ProductDemandDescFlowVO convert(ProductDemandDescFlowDO productDemandDescFlowDO);

    ProductDemandDescFlowDO clone(ProductDemandDescFlowDO auditingFlow);
}

package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProductDemandDescRecordDO;
import com.timevale.forward.facade.api.result.ProductDemandDescRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author jingchun
 * create on 2022/7/4
 */
@Mapper
public interface ProductDemandDescRecordCopier {

    ProductDemandDescRecordCopier INSTANCE = Mappers.getMapper(ProductDemandDescRecordCopier.class);

    ProductDemandDescRecordVO convert(ProductDemandDescRecordDO productDemandDescRecordDO);

    List<ProductDemandDescRecordVO> convert(List<ProductDemandDescRecordDO> productDemandDescRecordDO);

}

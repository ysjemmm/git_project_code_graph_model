package com.timevale.forward.service.copy;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @Date 2021/12/15 10:56
 */
@Mapper
public interface BizDemandCopier {
    BizDemandCopier INSTANCE = Mappers.getMapper(BizDemandCopier.class);


}

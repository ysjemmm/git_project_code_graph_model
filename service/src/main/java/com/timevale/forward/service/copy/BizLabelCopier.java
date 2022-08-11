package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.facade.api.request.BizLabelAddReq;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by xingyun
 * @date 2021/12/15 10:30
 */
@Mapper
public interface BizLabelCopier {

    BizLabelCopier INSTANCE = Mappers.getMapper(BizLabelCopier.class);


    /**
     *
     * @param bizLabelAddReq bizLabelAddReq
     * @return BizLabelDO
     */
    BizLabelDO convert(BizLabelAddReq bizLabelAddReq);


}

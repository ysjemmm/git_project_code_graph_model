package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.facade.api.result.BizDomainVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @Date 2021/12/15 10:27
 */
@Mapper
public interface BizDomainCopier {
    BizDomainCopier INSTANCE = Mappers.getMapper(BizDomainCopier.class);

    /**
     * 业务域 DO转换VO
     *
     * @param bizDomainDO 业务域DO
     * @return 业务域VO
     */
    BizDomainVO convert(BizDomainDO bizDomainDO);
}

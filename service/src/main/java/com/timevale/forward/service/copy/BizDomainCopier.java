package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.facade.api.result.BizDomainVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

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

    /**
     * 批量处理
     *
     * @param bizDomainDOList 业务域Do 列表
     * @return 业务域VO 列表
     */
    List<BizDomainVO> convert(List<BizDomainDO> bizDomainDOList);
}

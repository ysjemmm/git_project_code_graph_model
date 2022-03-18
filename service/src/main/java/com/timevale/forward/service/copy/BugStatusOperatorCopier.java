package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BugStatusOperatorDO;
import com.timevale.forward.facade.api.result.BugStatusOperatorVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @Date 2022/3/18 18:31
 * @Author 望轩
 */
@Mapper
public interface BugStatusOperatorCopier {
    BugStatusOperatorCopier INSTANCE = Mappers.getMapper(BugStatusOperatorCopier.class);

    /**
     * bugStatusOperatorDO --> BugStatusOperatorVO
     *
     * @param bugStatusOperatorDO 对象
     * @return BugStatusOperatorVO
     */
    BugStatusOperatorVO convert(BugStatusOperatorDO bugStatusOperatorDO);
}

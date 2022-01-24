package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.SubmitTestDO;
import com.timevale.forward.facade.api.result.SubmitTestVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @Date 2022/1/24 17:31
 * @Author 望轩
 */
@Mapper
public interface SubmitTestCopier {
    SubmitTestCopier INSTANCE = Mappers.getMapper(SubmitTestCopier.class);

    /**
     * SubmitTestDO -> SubmitTestVO
     *
     * @param submitTestDO 源对象
     * @return 目标对象
     */
    SubmitTestVO convert(SubmitTestDO submitTestDO);

}

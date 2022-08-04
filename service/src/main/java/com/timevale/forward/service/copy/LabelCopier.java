package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.facade.api.result.LabelSimpleVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
 */
@Mapper
public interface LabelCopier {

    LabelCopier INSTANCE = Mappers.getMapper(LabelCopier.class);


    /**
     *
     * @param labelDOList labelDOList
     * @return LabelSimpleVO
     */
    List<LabelSimpleVO> convert(List<LabelDO> labelDOList);


}

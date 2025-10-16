package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.LabelListCondition;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.facade.api.query.LabelQueryList;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.LabelSimpleVO;
import com.timevale.forward.facade.api.result.LabelVO;
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

    /**
     *
     * @param labelDOList
     * @return
     */
    List<BizLabelSimpleVO> convert2BizLabel(List<LabelDO> labelDOList);

    /**
     *
     * @param labelQueryList labelQueryList
     * @return LabelListCondition
     */
    LabelListCondition convert(LabelQueryList labelQueryList);

    /**
     *
     * @param labelDOList labelDOList
     * @return LabelVO
     */
    List<LabelVO> convertT(List<LabelDO> labelDOList);

}

package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.facade.api.result.ModelVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
 */
@Mapper
public interface ModelCopier {

    ModelCopier INSTANCE = Mappers.getMapper(ModelCopier.class);


    /**
     * 批量处理
     *
     * @param modelDOList 产品线do 列表
     * @return 列表
     */
    List<ModelVO> convert(List<ModelDO> modelDOList);


}

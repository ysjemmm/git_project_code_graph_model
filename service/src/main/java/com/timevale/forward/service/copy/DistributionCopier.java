package com.timevale.forward.service.copy;

import com.timevale.forward.dal.dto.UpdateTimeDTO;
import com.timevale.forward.facade.api.result.UpdateTimeVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2022/06/28 10:56
 */
@Mapper
public interface DistributionCopier {
    DistributionCopier INSTANCE = Mappers.getMapper(DistributionCopier.class);

    UpdateTimeVO convert(UpdateTimeDTO updateTimeDTO);
}

package com.timevale.forward.service.copy;

import com.timevale.forward.dal.dto.DevopsAppDTO;
import com.timevale.forward.dal.dto.DevopsProjectDTO;
import com.timevale.forward.dal.entity.DevopsAppDO;
import com.timevale.forward.facade.api.result.DevopsAppVO;
import com.timevale.forward.facade.api.result.DevopsProjectVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface DevopsAppCopier {
    DevopsAppCopier INSTANCE = Mappers.getMapper(DevopsAppCopier.class);

    DevopsAppDO dto2do(DevopsAppDTO appDTO, DevopsProjectDTO projectDTO, Long mainId);

    DevopsAppVO do2vo(DevopsAppDO devopsAppDO);

    List<DevopsAppVO> do2vo(List<DevopsAppDO> appDOList);

    DevopsProjectVO do2pvo(DevopsAppDO devopsAppDO);

    List<DevopsProjectVO> do2pvo(List<DevopsAppDO> appDOList);
}

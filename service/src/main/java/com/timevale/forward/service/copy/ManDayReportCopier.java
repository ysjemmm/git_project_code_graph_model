package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.dal.entity.ManDayReportDO;
import com.timevale.forward.facade.api.request.ManDayReportModifyReq;
import com.timevale.forward.facade.api.result.ManDayVO;
import com.timevale.forward.service.utils.date.DateUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;


@Mapper
public interface ManDayReportCopier {

    ManDayReportCopier INSTANCE = Mappers.getMapper(ManDayReportCopier.class);

    ManDayReportDO convert(ManDayReportModifyReq manDayReportModifyReq);

}

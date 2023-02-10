package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.ManDayReportCondition;
import com.timevale.forward.dal.entity.ManDayReportDO;
import com.timevale.forward.dal.entity.ManDayReportListDO;
import com.timevale.forward.facade.api.query.ManDayReportQueryList;
import com.timevale.forward.facade.api.request.ManDayReportModifyReq;
import com.timevale.forward.facade.api.result.ManDayReportListVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@Mapper
public interface ManDayReportCopier {

    ManDayReportCopier INSTANCE = Mappers.getMapper(ManDayReportCopier.class);

    ManDayReportDO convert(ManDayReportModifyReq manDayReportModifyReq);

    ManDayReportCondition convert(ManDayReportQueryList manDayReportQueryList);

    ManDayReportListVO convert(ManDayReportListDO manDayReportListDO);
}

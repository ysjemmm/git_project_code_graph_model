package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.HistoryRecordDO;
import com.timevale.forward.facade.api.result.HistoryRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface HistoryRecordCopier {

    HistoryRecordCopier INSTANCE = Mappers.getMapper(HistoryRecordCopier.class);

    HistoryRecordVO do2vo(HistoryRecordDO recordDO);

    List<HistoryRecordVO> do2vo(List<HistoryRecordDO> recordDOList);

}

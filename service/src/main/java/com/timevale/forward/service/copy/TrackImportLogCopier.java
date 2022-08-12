package com.timevale.forward.service.copy;

import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.forward.dal.entity.TrackImportLogDO;
import com.timevale.forward.facade.api.result.TrackImportLogFileVO;
import com.timevale.forward.facade.api.result.TrackImportLogListVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


/**
 * @author by YangXu
 * @date 2022/08/12 14:28
 */
@Mapper
public interface TrackImportLogCopier {

    TrackImportLogCopier INSTANCE = Mappers.getMapper(TrackImportLogCopier.class);

    TrackImportLogListVO convert(TrackImportLogDO trackImportLogDO);

    TrackImportLogFileVO convert(FileDownloadDTO fileDownloadDTO);
}

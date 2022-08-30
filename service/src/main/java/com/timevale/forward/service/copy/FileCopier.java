package com.timevale.forward.service.copy;

import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.result.FileVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface FileCopier {

    FileCopier INSTANCE = Mappers.getMapper(FileCopier.class);

    /**
     * 批量转换转换DO
     *
     * @param list 列表
     * @return FileDO列表
     */
    List<FileDO> convert(List<FileAddReq> list);


    /**
     * 批量转换转换DO
     *
     * @param list 列表
     * @return FileDO列表
     */
    List<FileVO> transform(List<FileDO> list);

    /**
     * 单个转换DO -> VO
     *
     * @param fileDO 参数
     * @return FileVO对象
     */
    @Mapping(source = "type", target = "fileType")
    FileVO change(FileDO fileDO);

    FileVO convert(FileDownloadDTO fileDownloadDTO);

}

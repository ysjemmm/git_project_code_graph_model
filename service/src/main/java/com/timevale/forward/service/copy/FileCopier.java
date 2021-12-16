package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import org.mapstruct.Mapper;
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

}

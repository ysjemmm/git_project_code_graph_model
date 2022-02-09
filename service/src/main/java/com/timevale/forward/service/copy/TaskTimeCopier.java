package com.timevale.forward.service.copy;

import com.timevale.forward.dal.dto.TaskTimeDTO;
import com.timevale.forward.facade.api.result.TaskTimeVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TaskTimeCopier {

    TaskTimeCopier INSTANCE = Mappers.getMapper(TaskTimeCopier.class);


    /**
     *
     * @param taskTimeDTO taskTimeDTO
     * @return TaskTimeVO
     */
    List<TaskTimeVO> convert(List<TaskTimeDTO> taskTimeDTO);



}

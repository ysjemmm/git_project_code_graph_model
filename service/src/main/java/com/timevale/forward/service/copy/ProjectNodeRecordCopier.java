package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectNodeRecordDO;
import com.timevale.forward.facade.api.result.ProjectNodeRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProjectNodeRecordCopier {

    ProjectNodeRecordCopier INSTANCE = Mappers.getMapper(ProjectNodeRecordCopier.class);
    /**
     *
     * @param projectNodeRecordDOList projectNodeRecordDOList
     * @return ProjectNodeRecordVO
     */
    List<ProjectNodeRecordVO> convert(List<ProjectNodeRecordDO> projectNodeRecordDOList);

    /**
     *
     * @param projectNodeRecordDO projectNodeRecordDO
     * @return ProjectNodeRecordVO
     */
    ProjectNodeRecordVO convert(ProjectNodeRecordDO projectNodeRecordDO);

}

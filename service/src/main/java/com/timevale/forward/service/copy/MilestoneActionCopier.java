package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneActionVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MilestoneActionCopier {
    MilestoneActionCopier INSTANCE = Mappers.getMapper(MilestoneActionCopier.class);

    List<ProjectMilestoneActionVO> task2vo(List<TaskDO> tasks);

    List<ProjectMilestoneActionVO> project2vo(List<ProjectDO> projects);
}

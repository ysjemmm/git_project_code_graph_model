package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.ProjectStageEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author jingchun
 * created on 2023/2/7
 */
@Mapper(imports = {
        ProjectStageEnum.class
})
public interface ProjectMilestoneCopier {

    ProjectMilestoneCopier INSTANCE = Mappers.getMapper(ProjectMilestoneCopier.class);

    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "id", source = "milestone.id")
    @Mapping(target = "type", source = "milestone.type")
    @Mapping(target = "stage", source = "milestone.stage")
    @Mapping(target = "stageName", expression = "java(ProjectStageEnum.getTextByCode(milestone.getStage()))")
    @Mapping(target = "relationName", source = "relateProject.name")
    ProjectMilestoneVO convert(ProjectMilestone milestone, ProjectDO relateProject);

    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "id", source = "milestone.id")
    @Mapping(target = "type", source = "milestone.type")
    @Mapping(target = "stage", source = "milestone.stage")
    @Mapping(target = "stageName", expression = "java(ProjectStageEnum.getTextByCode(milestone.getStage()))")
    @Mapping(target = "relationName", source = "task.name")
    ProjectMilestoneVO convert(ProjectMilestone milestone, TaskDO task);

}

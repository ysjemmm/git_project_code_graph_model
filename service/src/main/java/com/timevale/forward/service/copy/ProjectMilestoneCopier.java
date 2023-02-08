package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.ProjectStageEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author jingchun
 * created on 2023/2/7
 */
@Mapper(imports = {
        ProjectStageEnum.class,
        ProjectStatusEnum.class,
        TaskStatusEnum.class
})
public interface ProjectMilestoneCopier {

    ProjectMilestoneCopier INSTANCE = Mappers.getMapper(ProjectMilestoneCopier.class);

    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "id", source = "milestone.id")
    @Mapping(target = "type", source = "milestone.type")
    @Mapping(target = "stage", source = "milestone.stage")
    @Mapping(target = "relationName", source = "relateProject.name")
    @Mapping(target = "stageName", expression = "java(ProjectStageEnum.getTextByCode(milestone.getStage()))")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(relateProject.getStatus()))")
    ProjectMilestoneVO convert(ProjectMilestone milestone, ProjectDO relateProject);

    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "id", source = "milestone.id")
    @Mapping(target = "type", source = "milestone.type")
    @Mapping(target = "stage", source = "milestone.stage")
    @Mapping(target = "relationName", source = "task.name")
    @Mapping(target = "statusName", expression = "java(TaskStatusEnum.getTextByCode(task.getStatus()))")
    @Mapping(target = "stageName", expression = "java(ProjectStageEnum.getTextByCode(milestone.getStage()))")
    ProjectMilestoneVO convert(ProjectMilestone milestone, TaskDO task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modifyManId", ignore = true)
    @Mapping(target = "modifyMan", ignore = true)
    @Mapping(target = "modifyDate", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createManId", ignore = true)
    @Mapping(target = "createMan", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    ProjectMilestone convert(ProjectMilestoneAddReq req);

}

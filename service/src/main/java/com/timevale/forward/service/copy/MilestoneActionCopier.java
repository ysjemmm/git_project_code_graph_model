package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneActionVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(imports = {
        TaskStatusEnum.class,
        ProjectStatusEnum.class,
        MilestoneTypeEnum.class
})
public interface MilestoneActionCopier {
    MilestoneActionCopier INSTANCE = Mappers.getMapper(MilestoneActionCopier.class);

    @Mapping(target = "principal", ignore = true)
    @Mapping(target = "principalId", ignore = true)
    @Mapping(target = "type", expression = "java(MilestoneTypeEnum.TASK.getCode())")
    @Mapping(target = "statusName", expression = "java(TaskStatusEnum.getTextByCode(task.getStatus()))")
    ProjectMilestoneActionVO task2vo(TaskDO task);

    List<ProjectMilestoneActionVO> task2vo(List<TaskDO> tasks);

    @Mapping(target = "principal", source = "pm")
    @Mapping(target = "principalId", source = "pmId")
    @Mapping(target = "type", expression = "java(MilestoneTypeEnum.PROJECT.getCode())")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(project.getStatus()))")
    ProjectMilestoneActionVO project2vo(ProjectDO project);

    List<ProjectMilestoneActionVO> project2vo(List<ProjectDO> projects);


}

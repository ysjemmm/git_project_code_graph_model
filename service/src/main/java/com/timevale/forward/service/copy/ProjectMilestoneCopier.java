package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.request.ProjectMilestoneModifyReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.ProjectStageEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.mq.dto.MilestoneDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * created on 2023/2/7
 */
@Mapper(imports = {
        ProjectStageEnum.class,
        ProjectStatusEnum.class,
        TaskStatusEnum.class,
        Collectors.class,
        MilestoneTypeEnum.class
})
public interface ProjectMilestoneCopier {

    ProjectMilestoneCopier INSTANCE = Mappers.getMapper(ProjectMilestoneCopier.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modifyManId", ignore = true)
    @Mapping(target = "modifyMan", ignore = true)
    @Mapping(target = "modifyDate", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createManId", ignore = true)
    @Mapping(target = "createMan", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    ProjectMilestone convert(ProjectMilestoneAddReq req);


    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "actualStartDate", ignore = true)
    @Mapping(target = "actualEndDate", ignore = true)
    @Mapping(target = "stageName", expression = "java(ProjectStageEnum.getTextByCode(milestone.getStage()))")
    ProjectMilestoneVO convert(ProjectMilestone milestone);

    @Mapping(target = "milestoneRelationId", source = "id")
    @Mapping(target = "milestoneType", expression = "java(MilestoneTypeEnum.TASK.getCode())")
    @Mapping(target = "suspend", expression = "java(TaskStatusEnum.SUSPEND.getCode().equals(taskDO.getStatus()))")
    @Mapping(target = "invalid", expression = "java(TaskStatusEnum.INVALID.getCode().equals(taskDO.getStatus()))")
    MilestoneDTO task2dto(TaskDO taskDO);

    @Mapping(target = "milestoneRelationId", source = "id")
    @Mapping(target = "milestoneType", expression = "java(MilestoneTypeEnum.PROJECT.getCode())")
    @Mapping(target = "suspend", expression = "java(ProjectStatusEnum.SUSPEND.getCode().equals(projectDO.getStatus()))")
    @Mapping(target = "invalid", expression = "java(ProjectStatusEnum.INVALID.getCode().equals(projectDO.getStatus()))")
    MilestoneDTO project2dto(ProjectDO projectDO);

    @Mapping(target = "milestoneName", source = "taskDO.name")
    @Mapping(target = "type", expression = "java(MilestoneTypeEnum.TASK.getCode())")
    @Mapping(target = "isDeleted", ignore = true)
    ProjectMilestone task2do(TaskDO taskDO);

    ProjectMilestone req2do(ProjectMilestoneModifyReq req);

    List<ProjectMilestoneVO> convert(List<ProjectMilestone> milestones);

}

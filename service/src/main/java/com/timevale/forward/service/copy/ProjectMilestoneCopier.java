package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.request.ProjectMilestoneModifyReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.ProjectStageEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
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
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "modifyMan", ignore = true)
    @Mapping(target = "modifyDate", ignore = true)
    @Mapping(target = "createMan", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "relationId", ignore = true)
    @Mapping(target = "modifyManId", ignore = true)
    @Mapping(target = "createManId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    ProjectMilestone convert(ProjectMilestoneAddReq req);


    @Mapping(target = "actions", ignore = true)
    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "stageName", expression = "java(ProjectStageEnum.getTextByCode(milestone.getStage()))")
    ProjectMilestoneVO convert(ProjectMilestone milestone);

    ProjectMilestone req2do(ProjectMilestoneModifyReq req);

    List<ProjectMilestoneVO> convert(List<ProjectMilestone> milestones);

}

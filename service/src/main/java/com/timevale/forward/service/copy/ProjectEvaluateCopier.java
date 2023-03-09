package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.EvaluateDimensionDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectEvaluateDO;
import com.timevale.forward.facade.api.request.EvaluateReq;
import com.timevale.forward.facade.api.result.ConclusionFormVO;
import com.timevale.forward.facade.api.result.ProjectEvaluateItemVO;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.model.enums.ProjectLevelEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.ProjectTypeEnum;
import com.timevale.forward.service.integration.epeius.model.ConclusionVar;
import com.timevale.forward.service.integration.epeius.model.ProjectEvaluateVar;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;


/**
 * @author by YangXu
 * @date 2023/03/06 18:44
 */
@Mapper(
        imports = {
                ProjectKindEnum.class,
                ProjectTypeEnum.class,
                ProjectLevelEnum.class,
                ProjectStatusEnum.class,
        }
)
public interface ProjectEvaluateCopier {
    ProjectEvaluateCopier INSTANCE = Mappers.getMapper(ProjectEvaluateCopier.class);

    ProjectEvaluateItemVO do2vo(ProjectEvaluateDO evaluateDO);

    List<ProjectEvaluateItemVO> do2vo(List<ProjectEvaluateDO> evaluateDOList);

    ProjectEvaluateDO req2do(EvaluateReq evaluateReq);

    List<ProjectEvaluateDO> req2do(Collection<EvaluateReq> evaluateReqs);

    ProjectEvaluateItemVO do2item(ProjectEvaluateDO evaluateDO, EvaluateDimensionDO dimensionDO);


    @Mapping(target = "kindName", expression = "java(ProjectKindEnum.getTextByCode(projectDO.getKind()))")
    @Mapping(target = "typeName", expression = "java(ProjectTypeEnum.getTextByCode(projectDO.getType()))")
    @Mapping(target = "levelName", expression = "java(ProjectLevelEnum.getTextByCode(projectDO.getLevel()))")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(projectDO.getStatus()))")
    ConclusionFormVO do2vo(ProjectDO projectDO);

    @Mapping(target = "projectId", source = "id")
    @Mapping(target = "projectName", source = "name")
    @Mapping(target = "kindName", expression = "java(ProjectKindEnum.getTextByCode(projectDO.getKind()))")
    @Mapping(target = "typeName", expression = "java(ProjectTypeEnum.getTextByCode(projectDO.getType()))")
    @Mapping(target = "levelName", expression = "java(ProjectLevelEnum.getTextByCode(projectDO.getLevel()))")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(projectDO.getStatus()))")
    ConclusionVar do2var(ProjectDO projectDO);

    @Mapping(target = "dimensionId", source = "dimensionDO.id")
    ProjectEvaluateVar do2var(ProjectEvaluateDO evaluateDO, EvaluateDimensionDO dimensionDO);

}

package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMemberEvaluateDO;
import com.timevale.forward.facade.api.request.MemberEvaluateModifyReq;
import com.timevale.forward.facade.api.request.MemberWorkloadModifyReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.MemberEvaluateVO;
import com.timevale.forward.facade.api.result.ProjectWorkloadChangeVO;
import com.timevale.forward.model.enums.GradeEnum;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.model.enums.ProjectLevelEnum;
import com.timevale.forward.model.enums.ProjectTypeEnum;
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
                GradeEnum.class,
                ProjectKindEnum.class,
                ProjectTypeEnum.class,
                ProjectLevelEnum.class,
        }
)
public interface ProjectMemberEvaluateCopier {
    ProjectMemberEvaluateCopier INSTANCE = Mappers.getMapper(ProjectMemberEvaluateCopier.class);

    @Mapping(target = "includeStat", constant = "true")
    @Mapping(target = "evaluateGrade", expression = "java(GradeEnum.B.getCode())")
    ProjectMemberEvaluateDO person2do(PersonAddReq personAddReq, Long projectId);

    MemberEvaluateVO do2vo(ProjectMemberEvaluateDO projectMemberEvaluateDO);

    List<MemberEvaluateVO> do2vo(Collection<ProjectMemberEvaluateDO> projectMemberEvaluateDO);

    ProjectMemberEvaluateDO req2do(MemberEvaluateModifyReq req);

    ProjectMemberEvaluateDO req2do(MemberWorkloadModifyReq req);
    ProjectMemberEvaluateDO req2do(MemberWorkloadModifyReq req, Long projectId);

    @Mapping(source = "id", target = "projectId")
    @Mapping(source = "name", target = "projectName")
    @Mapping(target = "kindName", expression = "java(ProjectKindEnum.getTextByCode(projectDO.getKind()))")
    @Mapping(target = "typeName", expression = "java(ProjectTypeEnum.getTextByCode(projectDO.getType()))")
    @Mapping(target = "levelName", expression = "java(ProjectLevelEnum.getTextByCode(projectDO.getLevel()))")
    ProjectWorkloadChangeVO do2vo(ProjectDO projectDO);
}



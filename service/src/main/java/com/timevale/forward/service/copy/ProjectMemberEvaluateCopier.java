package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMemberEvaluateDO;
import com.timevale.forward.facade.api.request.MemberEvaluateModifyReq;
import com.timevale.forward.facade.api.request.MemberWorkloadModifyReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.ConclusionMemberItemVO;
import com.timevale.forward.facade.api.result.MemberEvaluateVO;
import com.timevale.forward.facade.api.result.ProjectWorkloadChangeVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.integration.epeius.model.MemberEvaluateVar;
import com.timevale.forward.service.integration.epeius.model.ProjectMemberEvaluateVar;
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
                YesOrNoEnum.class,
                ProjectKindEnum.class,
                ProjectTypeEnum.class,
                ProjectLevelEnum.class,
        }
)
public interface ProjectMemberEvaluateCopier {
    ProjectMemberEvaluateCopier INSTANCE = Mappers.getMapper(ProjectMemberEvaluateCopier.class);

    ProjectMemberEvaluateDO person2do(PersonAddReq personAddReq);

    @Mapping(target = "evaluateGradeName", expression = "java(GradeEnum.getTextByCode(memberEvaluateDO.getEvaluateGrade()))")
    MemberEvaluateVO do2vo(ProjectMemberEvaluateDO memberEvaluateDO);

    List<MemberEvaluateVO> do2vo(Collection<ProjectMemberEvaluateDO> projectMemberEvaluateDO);

    @Mapping(target = "evaluateGradeName", expression = "java(GradeEnum.getTextByCode(memberEvaluateDO.getEvaluateGrade()))")
    ConclusionMemberItemVO do2cvo(ProjectMemberEvaluateDO memberEvaluateDO);

    List<ConclusionMemberItemVO> do2cvo(Collection<ProjectMemberEvaluateDO> projectMemberEvaluateDO);

    ProjectMemberEvaluateDO req2do(MemberEvaluateModifyReq req);

    ProjectMemberEvaluateDO req2do(MemberWorkloadModifyReq req);
    ProjectMemberEvaluateDO req2do(MemberWorkloadModifyReq req, Long projectId);

    @Mapping(source = "id", target = "projectId")
    @Mapping(source = "name", target = "projectName")
    @Mapping(target = "kindName", expression = "java(ProjectKindEnum.getTextByCode(projectDO.getKind()))")
    @Mapping(target = "typeName", expression = "java(ProjectTypeEnum.getTextByCode(projectDO.getType()))")
    @Mapping(target = "levelName", expression = "java(ProjectLevelEnum.getTextByCode(projectDO.getLevel()))")
    ProjectWorkloadChangeVO do2vo(ProjectDO projectDO);

    @Mapping(target = "evaluateGradeName", expression = "java(GradeEnum.getTextByCode(memberEvaluateDO.getEvaluateGrade()))")
    ProjectMemberEvaluateVar do2var(ProjectMemberEvaluateDO memberEvaluateDO);

    List<ProjectMemberEvaluateVar> do2var(List<ProjectMemberEvaluateDO> memberEvaluateDOs);

    MemberEvaluateVar vo2var(MemberEvaluateVO memberEvaluateVO);

    List<MemberEvaluateVar> do2var(Collection<MemberEvaluateVO> memberEvaluateVOs);
}



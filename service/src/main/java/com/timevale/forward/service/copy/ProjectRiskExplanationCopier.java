package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectRiskExplanationDO;
import com.timevale.forward.facade.api.request.ProjectRiskExplanationAddReq;
import com.timevale.forward.facade.api.result.ProjectRiskExplanationVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2021/12/15 10:27
 */
@Mapper
public interface ProjectRiskExplanationCopier {
    ProjectRiskExplanationCopier INSTANCE = Mappers.getMapper(ProjectRiskExplanationCopier.class);

    ProjectRiskExplanationVO convert(ProjectRiskExplanationDO projectRiskExplanationDO);

    ProjectRiskExplanationDO convert(ProjectRiskExplanationAddReq projectRiskExplanationAddReq);
}

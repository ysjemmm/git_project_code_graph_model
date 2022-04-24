package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.facade.api.request.ProjectRiskAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskModifyReq;
import com.timevale.forward.facade.api.result.ProjectRiskVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2021/12/15 10:27
 */
@Mapper
public interface ProjectRiskCopier {
    ProjectRiskCopier INSTANCE = Mappers.getMapper(ProjectRiskCopier.class);

    ProjectRiskDO convert(ProjectRiskAddReq projectRiskAddReq);

    ProjectRiskDO convert(ProjectRiskModifyReq projectRiskModifyReq);

    ProjectRiskVO convert(ProjectRiskDO projectRiskDO);
}

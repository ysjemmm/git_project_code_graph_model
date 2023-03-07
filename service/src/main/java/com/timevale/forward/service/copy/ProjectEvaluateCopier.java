package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.EvaluateDimensionDO;
import com.timevale.forward.dal.entity.ProjectEvaluateDO;
import com.timevale.forward.facade.api.request.EvaluateReq;
import com.timevale.forward.facade.api.result.ProjectEvaluateItemVO;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.model.enums.ProjectLevelEnum;
import com.timevale.forward.model.enums.ProjectTypeEnum;
import org.mapstruct.Mapper;
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
        }
)
public interface ProjectEvaluateCopier {
    ProjectEvaluateCopier INSTANCE = Mappers.getMapper(ProjectEvaluateCopier.class);

    ProjectEvaluateItemVO do2vo(ProjectEvaluateDO evaluateDO);

    List<ProjectEvaluateItemVO> do2vo(List<ProjectEvaluateDO> evaluateDOList);

    ProjectEvaluateDO req2do(EvaluateReq evaluateReq);

    List<ProjectEvaluateDO> req2do(Collection<EvaluateReq> evaluateReqs);

    ProjectEvaluateItemVO do2vo(ProjectEvaluateDO evaluateDO, EvaluateDimensionDO dimensionDO);

}

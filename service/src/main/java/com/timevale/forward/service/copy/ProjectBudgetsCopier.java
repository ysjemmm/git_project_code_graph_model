package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectBudgetDO;
import com.timevale.forward.facade.api.request.ProjectBudgetSaveReq;
import com.timevale.forward.facade.api.result.ProjectBudgetVO;
import com.timevale.forward.model.middle.ProjectBudgetMD;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;


/**
 * @author by YangXu
 * @date 2023/02/03 16:00
 */
@Mapper
public interface ProjectBudgetsCopier {

    ProjectBudgetsCopier INSTANCE = Mappers.getMapper(ProjectBudgetsCopier.class);

    @Mapping(source = "projectId", target = "projectId")
    ProjectBudgetDO req2do(ProjectBudgetSaveReq req, Long projectId);

    ProjectBudgetDO req2do(ProjectBudgetSaveReq req);

    ProjectBudgetVO do2vo(ProjectBudgetDO budgetDO);

    ProjectBudgetMD do2md(ProjectBudgetDO budgetDO);

    List<ProjectBudgetVO> do2vo(List<ProjectBudgetDO> doList);
}

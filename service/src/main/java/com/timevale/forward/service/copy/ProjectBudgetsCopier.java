package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectBudgetDO;
import com.timevale.forward.facade.api.request.ProjectBudgetSaveReq;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


/**
 * @author by YangXu
 * @date 2023/02/03 16:00
 */
@Mapper
public interface ProjectBudgetsCopier {

    ProjectBudgetsCopier INSTANCE = Mappers.getMapper(ProjectBudgetsCopier.class);

    @Mapping(source = "projectId", target = "projectId")
    ProjectBudgetDO convert(ProjectBudgetSaveReq projectBudgetSaveReq, Long projectId);
}

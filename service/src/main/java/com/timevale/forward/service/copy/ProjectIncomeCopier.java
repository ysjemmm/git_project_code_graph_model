package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectIncomeDO;
import com.timevale.forward.facade.api.request.ProjectIncomeSaveReq;
import com.timevale.forward.facade.api.result.ProjectDocumentVO;
import com.timevale.forward.facade.api.result.ProjectIncomeVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:23
 */
@Mapper
public interface ProjectIncomeCopier {

    ProjectIncomeCopier INSTANCE = Mappers.getMapper(ProjectIncomeCopier.class);

    ProjectIncomeVO do2Vo(ProjectIncomeDO projectIncomeDO);

    List<ProjectIncomeVO> do2Vo(List<ProjectIncomeDO> projectIncomeDOs);

    ProjectIncomeDO req2Do(ProjectIncomeSaveReq req);
}

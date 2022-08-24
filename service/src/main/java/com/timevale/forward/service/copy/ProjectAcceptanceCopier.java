package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.ProjectAcceptanceListCondition;
import com.timevale.forward.dal.entity.ProjectAcceptanceDO;
import com.timevale.forward.facade.api.query.ProjectAcceptanceQueryList;
import com.timevale.forward.facade.api.result.ProjectAcceptanceVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
 */
@Mapper
public interface ProjectAcceptanceCopier {

    ProjectAcceptanceCopier INSTANCE = Mappers.getMapper(ProjectAcceptanceCopier.class);

    /**
     *
     * @param query query
     * @return return
     */
    ProjectAcceptanceListCondition convert(ProjectAcceptanceQueryList query);


    /**
     *
     * @param projectAcceptanceDOList projectAcceptanceDOList
     * @return return
     */
    List<ProjectAcceptanceVO> convert(List<ProjectAcceptanceDO>projectAcceptanceDOList);

}

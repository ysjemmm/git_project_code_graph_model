package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectGoalDO;
import com.timevale.forward.facade.api.request.ProjectGoalAddReq;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author jingchun
 * create on 2022/6/21
 */
@Mapper
public interface ProjectGoalCopier {


    ProjectGoalCopier INSTANCE = Mappers.getMapper(ProjectGoalCopier.class);

    ProjectGoalDO convert(ProjectGoalAddReq projectGoalAddReq);

}

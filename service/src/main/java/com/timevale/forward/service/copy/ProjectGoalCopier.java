package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectGoalDO;
import com.timevale.forward.facade.api.request.ProjectGoalAddReq;
import com.timevale.forward.facade.api.request.ProjectGoalModifyReq;
import com.timevale.forward.facade.api.result.ProjectGoalVO;
import com.timevale.forward.model.middle.ProjectGoalMD;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author jingchun
 * create on 2022/6/21
 */
@Mapper
public interface ProjectGoalCopier {


    ProjectGoalCopier INSTANCE = Mappers.getMapper(ProjectGoalCopier.class);

    ProjectGoalDO convert(ProjectGoalAddReq projectGoalAddReq);

    List<ProjectGoalDO> convert(List<ProjectGoalAddReq> projectGoalAddReq);

    // 目标更新时不允许更新主目标
    ProjectGoalDO convert(ProjectGoalModifyReq projectGoalModifyReq);

    ProjectGoalMD convert(ProjectGoalDO projectGoalDO);

    ProjectGoalVO convert2VO(ProjectGoalDO projectGoalDO);
    List<ProjectGoalVO> convert2VO(List<ProjectGoalDO> projectGoalDO);

}

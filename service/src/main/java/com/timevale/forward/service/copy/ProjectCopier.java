package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProjectCopier {

    ProjectCopier INSTANCE = Mappers.getMapper(ProjectCopier.class);

    /**
     * 转换转换DO
     *
     * @param projectAddReq 对象
     * @return ProductDemandDO
     */
    ProjectDO convert(ProjectAddReq projectAddReq);
    /**
     * 转换转换DO
     *
     * @param projectModifyReq 对象
     * @return ProjectDO
     */
    ProjectDO convert(ProjectModifyReq projectModifyReq);
    /**
     * 转换转换DO
     *
     * @param projectDO 对象
     * @return ProjectDetailVO
     */
    ProjectDetailVO convert(ProjectDO projectDO);

}

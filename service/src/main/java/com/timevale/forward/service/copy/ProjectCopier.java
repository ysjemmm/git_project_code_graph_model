package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProjectAddReq;
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

}

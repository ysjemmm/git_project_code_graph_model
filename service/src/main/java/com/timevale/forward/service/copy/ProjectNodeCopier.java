package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.request.ProjectNodeAddReq;
import com.timevale.forward.facade.api.result.ProjectNodeVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProjectNodeCopier {

    ProjectNodeCopier INSTANCE = Mappers.getMapper(ProjectNodeCopier.class);

    /**
     * 转换转换DO
     *
     * @param projectNodeAddReq 对象
     * @return ProductDemandDO
     */
    List<ProjectNodeDO> convert(List<ProjectNodeAddReq> projectNodeAddReq);

    /**
     * 转换转换VO
     *
     * @param projectNodeDO 对象
     * @return ProjectNodeVO
     */
    List<ProjectNodeVO> transform(List<ProjectNodeDO> projectNodeDO);

}

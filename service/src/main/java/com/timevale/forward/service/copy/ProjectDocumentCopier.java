package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDocument;
import com.timevale.forward.facade.api.request.ProjectDocumentReq;
import com.timevale.forward.facade.api.result.ProjectDocumentVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:23
 */
@Mapper
public interface ProjectDocumentCopier {

    ProjectDocumentCopier INSTANCE = Mappers.getMapper(ProjectDocumentCopier.class);

    @Mapping(target = "files", ignore = true)
    ProjectDocumentVO do2Vo(ProjectDocument projectDocument);

    List<ProjectDocumentVO> do2Vo(List<ProjectDocument> projectDocument);

    ProjectDocument req2Do(ProjectDocumentReq req);
}

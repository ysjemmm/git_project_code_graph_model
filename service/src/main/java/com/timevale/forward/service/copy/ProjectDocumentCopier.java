package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ProjectDocument;
import com.timevale.forward.facade.api.request.ProjectDocumentReq;
import com.timevale.forward.facade.api.result.ProjectDocumentVO;
import com.timevale.forward.model.enums.BugBelongEnum;
import com.timevale.forward.model.enums.BugReasonEnum;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:23
 */
@Mapper
public interface ProjectDocumentCopier {

    ProjectDocumentCopier INSTANCE = Mappers.getMapper(ProjectDocumentCopier.class);

    ProjectDocumentVO do2Vo(ProjectDocument projectDocument);

    ProjectDocument req2Do(ProjectDocumentReq req);
}

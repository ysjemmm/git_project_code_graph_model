package com.timevale.forward.service.copy;


import com.timevale.forward.dal.entity.ProjectDocument;
import com.timevale.forward.facade.api.request.ProjectDocumentReq;
import com.timevale.forward.facade.api.result.ProjectDocumentVO;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:23
 */
public interface ProjectDocumentCopier {

    ProjectDocumentCopier INSTANCE = Mappers.getMapper(ProjectDocumentCopier.class);

    ProjectDocumentVO do2Vo(ProjectDocument projectDocument);

    ProjectDocument req2Do(ProjectDocumentReq req);
}

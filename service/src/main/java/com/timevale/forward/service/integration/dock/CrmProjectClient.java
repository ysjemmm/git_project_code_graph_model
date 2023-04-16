package com.timevale.forward.service.integration.dock;

import com.timevale.crm.dock.facade.api.HybridCloudProjectService;
import com.timevale.crm.dock.facade.model.output.ProjectOutput;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class CrmProjectClient {

    private final HybridCloudProjectService hybridCloudProjectService;

    public Optional<ProjectOutput> getProject(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            return Optional.empty();
        }
        try {
            BaseResult<ProjectOutput> res = hybridCloudProjectService.getProject(projectId);
            if (res.ifSuccess()) {
                return Optional.ofNullable(res.getData());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("get crm-dock project failed, projectId: {}, msg: {}", projectId, e.getMessage());
            return Optional.empty();
        }
    }

}

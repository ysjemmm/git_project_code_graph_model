package com.timevale.forward.service.integration.encourage;

import com.timevale.encourage.facade.api.EncouragePbgApi;
import com.timevale.encourage.facade.api.response.GetProjectPointResponse;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/05/17 10:57
 */
@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class EncourageClient {
    private final EncouragePbgApi encouragePbgApi;

    public Optional<GetProjectPointResponse> getProjectPoint(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        try {
            BaseResult<GetProjectPointResponse> res = encouragePbgApi.getProjectPoint(projectId.toString());
            if (res.ifSuccess()) {
                return Optional.ofNullable(res.getData());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("get get projectPoint failed, projectId: {}, msg: {}", projectId, e.getMessage());
            return Optional.empty();
        }
    }
}

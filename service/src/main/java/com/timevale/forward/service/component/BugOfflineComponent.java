package com.timevale.forward.service.component;

import java.util.List;

public interface BugOfflineComponent {
    /**
     *
     * @param projectId 项目id
     * @param productLineIdsInProject 产品线id
     */
    void containProductLineInBugOffline(Long projectId,List<Long> productLineIdsInProject);

}

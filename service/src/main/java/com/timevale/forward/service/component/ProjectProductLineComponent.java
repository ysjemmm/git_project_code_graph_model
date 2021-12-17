package com.timevale.forward.service.component;

import java.util.List;

public interface ProjectProductLineComponent {
    /**
     * 
     * @param list 产品线id
     * @param projectId 项目id
     */
    void add(List<Long> list,Long projectId);

}

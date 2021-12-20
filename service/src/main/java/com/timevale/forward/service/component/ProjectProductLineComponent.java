package com.timevale.forward.service.component;

import java.util.List;

public interface ProjectProductLineComponent {
    /**
     * 
     * @param list 产品线id
     * @param projectId 项目id
     */
    void add(List<Long> list,Long projectId);

    /**
     *
     * @param list 产品线id
     * @param projectId 项目id
     */
    void update(List<Long> list,Long projectId);

    /**
     * 节点信息
     * @param projectId 项目id
     * @return ids
     */
    List<Long> get(Long projectId);

}

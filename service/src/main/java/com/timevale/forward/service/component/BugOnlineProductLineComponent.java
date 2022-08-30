package com.timevale.forward.service.component;

import java.util.List;

/**
 * @Date 2022/3/22 10:29
 * @Author 望轩
 */
public interface BugOnlineProductLineComponent {
    /**
     * 更新
     *
     * @param productLineIdList 产品线列表
     * @param id                线上bug的id
     */
    void update(List<Long> productLineIdList, Long id);

    /**
     * 更新
     *
     * @param productLineIdList 产品线列表
     * @param id                线上bug的id
     */
    void add(List<Long> productLineIdList, Long id);
}

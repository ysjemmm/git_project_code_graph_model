package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BugOnlineStatusOperatorDO;

/**
 * 线上bug状态操作记录
 * @author yexuan
 * @date 2022-12-08 14:28 yexuan
 */
public interface BugOnlineStatusOperatorComponent {

    /**
     * 新增
     * @param bugOnlineStatusOperatorDO
     */
    void add(BugOnlineStatusOperatorDO bugOnlineStatusOperatorDO);

    /**
     * 删除
     * @param bugOnlineId
     */
    void delete(Long bugOnlineId);
}

package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BugLogDO;

import java.util.List;

public interface BugLogComponent {

    void insertToBugStatusOperator(Long bugId, String userId, String userName,Integer bugType);

    void insertToBugStatusOperator(Long bugOnlineId, String userId, String userName);

    void bugOnlineInit(Long id);

    void add(BugLogDO bugLogDO);

    void add(List<BugLogDO> bugLogDOList);

    /**
     * 更新线上bug原因日志
     *
     * @param bugId     线上bug id
     * @param oldReason 老bug原因
     * @param newReason 新bug原因
     */
    void reason(Long bugId, Integer oldReason, Integer newReason);

    /**
     * 更新关联线下bug日志
     *
     * @param bugId           线上bug id
     * @param oldBugOfflineId 断开关联的线下bug id
     * @param newBugOfflineId 关联的线下bug id
     */
    void bugOffline(Long bugId, Long oldBugOfflineId, Long newBugOfflineId);

    /**
     * 客开项目日志
     *
     * @param bugOnlineId 线上bug id
     * @param oldSourceId 老源id
     * @param newSourceId 新源id
     */
    void customDevProject(Long bugOnlineId, String oldSourceId, String newSourceId);

    /**
     * 操作符
     * 经办人
     *
     * @param bugOnlineId 线上bug id
     * @param oldValue    旧值
     * @param newValue    新值
     */
    void operator(Long bugOnlineId, String oldValue, String newValue);
}

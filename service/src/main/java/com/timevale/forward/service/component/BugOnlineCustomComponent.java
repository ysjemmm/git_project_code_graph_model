package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BugOnlineCustomDO;
import com.timevale.forward.facade.api.request.BugOnlineCustomAddReq;

import java.util.List;

/**
 * @author yexuan
 * @date 2022-09-23 15:26 yexuan
 */
public interface BugOnlineCustomComponent {

    /**
     * 新增
     * @param addReqList
     */
    void add(List<BugOnlineCustomAddReq> addReqList, Long bugOnlineId);

    /**
     * 更新
     * @param addReqList
     * @param bugOnlineId
     */
    void update(List<BugOnlineCustomAddReq> addReqList, Long bugOnlineId);


    /**
     * 根据线上bug id查询
     * @param bugOnlineId
     * @return
     */
    List<BugOnlineCustomDO> selectByBugOnlineId(Long bugOnlineId);

    /**
     * 删除
     * @param bugOnlineId
     */
    void delete(Long bugOnlineId);
}

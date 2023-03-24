package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BugLogDO;

import java.util.List;

public interface BugLogComponent {

    void insertToBugStatusOperator(Long bugId, String userId, String userName,Integer bugType);

    void insertToBugStatusOperator(Long bugOnlineId, String userId, String userName);

    void bugOnlineInit(Long id);

    void add(BugLogDO bugLogDO);

    void add(List<BugLogDO> bugLogDOList);
}

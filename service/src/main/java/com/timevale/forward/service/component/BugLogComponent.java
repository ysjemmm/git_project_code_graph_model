package com.timevale.forward.service.component;

public interface BugLogComponent {

    void insertToBugStatusOperator(Long bugId, String userId, String userName,Integer bugType);

    void insertToBugStatusOperator(Long bugOnlineId, String userId, String userName);
}

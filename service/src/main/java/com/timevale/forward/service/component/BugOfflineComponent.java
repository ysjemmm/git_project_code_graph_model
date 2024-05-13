package com.timevale.forward.service.component;

import java.util.Collection;

public interface BugOfflineComponent {

    /**
     * 批量变更经办人
     *
     * @param ids        线下bugIDd列表
     * @param operatorId 经办人id
     */
    void transferOperatorId(Collection<Long> ids, String operator, String operatorId);

    /**
     * 批量变更提出人
     *
     * @param ids        线下bugIDd列表
     * @param proposerId 提出人id
     */
    void transferProposerId(Collection<Long> ids, String proposer, String proposerId);



}

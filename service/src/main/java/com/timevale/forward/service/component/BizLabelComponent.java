package com.timevale.forward.service.component;

import java.util.List;

public interface BizLabelComponent {

    /**
     *
     * @param mainId mainId
     * @param type type
     * @param labelIds labelIds
     * @param add add
     */
    void addLog(Long mainId, List<Long> labelIds, Integer type ,Boolean add) ;

    /**
     *
     * @param bizId bizId
     * @param labelIds labelIds
     * @param type type
     */
    void addLabel(Long bizId,List<Long> labelIds, Integer type) ;

}

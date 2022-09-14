package com.timevale.forward.service.component;

import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.LabelSimpleVO;

import java.util.List;
import java.util.Map;

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

    /**
     * listLabelSimpleVO
     * @param bizIds
     * @param type
     * @return
     */
    Map<Long, List<BizLabelSimpleVO>> getBizLabelMap(List<Long> bizIds, Integer type);

}

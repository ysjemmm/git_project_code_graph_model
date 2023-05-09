package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;

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
     * 如果不存在则添加标签
     *
     * @param bizId   bizId
     * @param labelId 标签id
     * @param type    type
     */
    boolean addLabelNx(Long bizId, Long labelId, Integer type);

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

    /**
     *
     * @param bizId bizId
     * @param type type
     */
    void deleteLabel(Long bizId, Integer type) ;

    /**
     * 查询
     *
     * @param bizId 业务id
     * @param type  类型
     */
    List<BizLabelDO> get(Long bizId, Integer type);
}

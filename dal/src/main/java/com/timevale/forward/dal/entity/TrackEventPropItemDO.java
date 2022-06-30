package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/16 13:42
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TrackEventPropItemDO extends BaseDO {
    /**
     * 属性id
     */
    private Long trackPropId;

    /**
     * 事件id
     */
    private Long trackEventId;

    /**
     * cnName
     */
    private String cnName;

    /**
     * egName
     */
    private String egName;

    /**
     * flowId
     */
    private String flowId;

    /**
     * status
     */
    private String status;

    /**
     * type
     */
    private String type;

    /**
     * dataType
     */
    private String dataType;

}

package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 10:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TrackPropItemDO extends BaseDO {

    /**
     * cnName
     */
    private String cnName;

    /**
     * egName
     */
    private String egName;

    /**
     * status
     */
    private Integer status;

    /**
     * status
     */
    private String statusName;

    /**
     * type
     */
    private Integer type;

    /**
     * type
     */
    private String typeName;

    /**
     * dataType
     */
    private String dataType;

}

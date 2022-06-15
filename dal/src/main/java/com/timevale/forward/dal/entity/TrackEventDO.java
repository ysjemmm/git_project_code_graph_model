package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 10:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TrackEventDO extends BaseDO {

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
     * trackMapId
     */
    private Long trackMapId;

    /**
     * platform
     */
    private String platform;

    /**
     * apiName
     */
    private String apiName;

    /**
     * touchMoment
     */
    private String touchMoment;

    /**
     * env
     */
    private String env;


    /**
     * status
     */
    private Integer status;

    /**
     * failReason
     */
    private String failReason;

}

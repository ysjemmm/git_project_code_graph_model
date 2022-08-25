package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/15 10:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectAcceptanceDO extends BaseDO {

    /**
     * projectId
     */
    private Long projectId;


    /**
     * acceptorId
     */
    private String acceptorId;


    /**
     * acceptor
     */
    private String acceptor;


    /**
     * status
     */
    private Integer status;


    /**
     * desc
     */
    private String desc;

    /**
     * acceptDate
     */
    private Date acceptDate;

}

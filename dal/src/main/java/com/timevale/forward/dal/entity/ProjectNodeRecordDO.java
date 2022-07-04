package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectNodeRecordDO extends BaseDO {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     *name
     */
    private String name;

    /**
     *planDate
     */
    private Date planDate;

    /**
     * version
     */
    private BigDecimal version;

}

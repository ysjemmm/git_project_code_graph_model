package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by xingyun
 * @date 2021/12/15 10:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LabelCategoryDO extends BaseDO {

    /**
     * name
     */
    private String name;

    /**
     * type
     */
    private String type;

    /**
     * markMan
     */
    private String markMan;

    /**
     * markManId
     */
    private String markManId;

    /**
     * deptId
     */
    private String deptId;


}

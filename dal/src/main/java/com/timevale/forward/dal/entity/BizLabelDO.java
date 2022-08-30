package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by xingyun
 * @date 2021/12/15 10:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizLabelDO extends BaseDO {

    /**
     * name
     */
    private Long bizId;

    /**
     * type
     */
    private Long labelId;

    /**
     * type
     */
    private Integer type;


}

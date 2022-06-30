package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 10:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TrackMapDO extends BaseDO {

    /**
     * name
     */
    private String name;

    /**
     * level
     */
    private Integer level;

    /**
     * 上级菜单id
     */
    private Long parentId;

}

package com.timevale.forward.dal.condition;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/15 10:03
 */
@Data
@SuperBuilder
public class BaseCondition {
    /**
     * 主键
     */
    private Long id;

    /**
     * 逻辑删除标识
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 修改时间
     */
    private Date modifyDate;

    /**
     * 创建人id
     */
    private String createManId;

    /**
     * 创建人
     */
    private String createMan;

    /**
     * 修改人id
     */
    private String modifyManId;

    /**
     * 修改人
     */
    private String modifyMan;

}

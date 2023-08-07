package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author jingchun
 * create on 8/7/2023
 **/
@Getter
@Setter
@Accessors(chain = true)
public class DingRelationDO {
    /**
     * 自增id
     */
    private Long id;

    /**
     * 关系类型 1-线上bug
     */
    private Integer relationType;

    /**
     * 关系id
     */
    private Long relationId;

    /**
     * 1-钉钉待办
     */
    private Integer dingType;

    /**
     * 钉钉id
     */
    private String dingId;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 修改时间
     */
    private Date modifyDate;
}

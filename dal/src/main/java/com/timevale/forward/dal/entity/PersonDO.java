package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 11:15
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PersonDO extends BaseDO {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 业务需求id
     */
    private Long bizDemandId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 用户类型
     */
    private Integer type;
}

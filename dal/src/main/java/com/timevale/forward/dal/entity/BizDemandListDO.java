package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/23 10:12
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizDemandListDO extends BaseDO {

    /**
     * 业务需求主题
     */
    private String name;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 业务域id
     */
    private Long bizDomainId;

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 预计上线时间
     */
    private Integer planReleaseDate;

    /**
     * 需求解决状态
     */
    private Integer status;

    /**
     * 接收人
     */
    private String receiveMan;

    /**
     * 接收人id
     */
    private String receiveManId;

    /**
     * 部门id
     */
    private Long deptId;
}


package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2022/04/24 15:45
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectRiskDO extends BaseDO {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 风险类型 0 其它， 10 项目关键节点逾期 20 提测质量不达标 30 任务逾期
     */
    private Integer type;

    /**
     * 名称
     */
    private String name;

    /**
     * 风险标志
     */
    private String sign;

    /**
     * 状态：0 待处理, 1 已处理
     */
    private Integer state;

    /**
     * 提交人
     */
    private String submitMan;

    /**
     * 提交人id
     */
    private String submitManId;

}

package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2022/04/29 16:27
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectRiskRecordDO extends BaseDO {

    /**
     * 主体id
     */
    private Long mainId;

    /**
     * 风险类型 0 其它， 10 项目关键节点逾期 20 提测质量不达标 30 任务逾期
     */
    private Integer type;

    /**
     * 名称
     */
    private String name;

    /**
     * 消息接收人
     */
    private String receiveMan;

    /**
     * 消息接收人
     */
    private String receiveManId;

}

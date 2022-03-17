package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 改进措施(ImprovementMeasureDO)实体类
 *
 * @author yangxu
 * @since 2022-03-16 18:00:50
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ImprovementMeasureDO extends BaseDO{

    /**
     * 故障单id
     */
    private Long troubleTicketId;

    /**
     * 事项描述
     */
    private String name;

    /**
     * 执行人
     */
    private String executor;

    /**
     * 执行人id
     */
    private String executorId;

    /**
     * 落实时间
     */
    private Date implementationTime;

    /**
     * 是否创建待办
     */
    private Boolean todo;

    /**
     * 待办id
     */
    private String todoId;

    /**
     * 状态
     */
    private Integer status;

}

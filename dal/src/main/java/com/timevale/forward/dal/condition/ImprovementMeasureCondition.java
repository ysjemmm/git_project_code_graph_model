package com.timevale.forward.dal.condition;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/03/19 11:10
 */
@Data
@SuperBuilder
public class ImprovementMeasureCondition extends BaseCondition {

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

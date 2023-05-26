package com.timevale.forward.dal.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @TableName project_milestone_action
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMilestoneActionDO extends BaseDO {

    /**
     * 里程碑id
     */
    private Long milestoneId;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 关系id
     */
    private Long relationId;
}
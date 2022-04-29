package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Objects;
import lombok.Data;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/01/24 13:54
 */
@Data
public class HomePageRiskWarningTaskDTO {

    /**
     * 项目id
     */
    @JSONField(name = "project_id")
    private Long projectId;

    /**
     * 主体id
     */
    @JSONField(name = "main_id")
    private Long mainId;

    /**
     * 风险类型
     */
    @JSONField(name = "judge_type")
    private Integer riskType;

    /**
     * 项目名称
     */
    @JSONField(name = "project_name")
    private String projectName;

    /**
     * 项目计划上线时间
     */
    @JSONField(name = "plan_end_date")
    private Date planEndDate;

    /**
     * 用户id
     */
    @JSONField(name = "user_id")
    private String userId;

    /**
     * 用户名
     */
    @JSONField(name = "user_name")
    private String userName;

    /**
     * 任务id
     */
    @JSONField(name = "task_id")
    private Long taskId;

    /**
     * 任务名称
     */
    @JSONField(name = "task_name")
    private String taskName;

    /**
     * 逾期类型
     */
    @JSONField(name = "due_type")
    private String overdueType;

    /**
     * 逾期时间
     */
    @JSONField(name = "task_duetime")
    private String overdueTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HomePageRiskWarningTaskDTO that = (HomePageRiskWarningTaskDTO) o;
        return Objects.equal(projectId, that.projectId) && Objects.equal(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(projectId, taskId);
    }
}

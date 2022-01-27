package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author by YangXu
 * @date 2022/01/24 13:54
 */
@Data
public class HomePageRiskWarningTaskDTO {

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
     * 项目id
     */
    @JSONField(name = "project_id")
    private Long projectId;

    /**
     * 项目名称
     */
    @JSONField(name = "project_name")
    private String projectName;

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
}

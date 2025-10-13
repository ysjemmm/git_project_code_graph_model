package com.timevale.forward.service.context;

import com.timevale.forward.dal.entity.AutomationRuleDO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 自动化规则执行上下文
 * 用于在规则触发时，向执行器传递必要的上下文信息
 */
@Data
public class AutomationExecutionContext {
    
    private AutomationRuleDO rule;

    /**
     * 被触发的业务对象，比如 Bug、Task、Project 等
     * 类型可以是 Object，或者泛型，后续可优化为泛型或具体类型
     * 例如：Bug 对象、Task 对象
     */
    private Object target;

    /**
     * 接收人 ID 列表，比如用户ID、邮箱、钉钉UserID等
     * 一般由规则中的 receiver_type + receiver_ids 决定
     * 例如：["user1", "user2"]
     */
    private List<String> receiverIds;

    /**
     * 附加参数，可用于模板渲染、业务逻辑判断等
     * 比如：项目名称、操作人、额外字段等
     */
    private Map<String, Object> extraParams;

    /**
     * 触发时间（可选，可用于记录或展示）
     */
    private LocalDateTime triggerTime;

    private String projectId;

    private String operatorId;

    private String bizKey;
}
package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AutomationRuleDO extends BaseDO {

    private String name;

    private String bizDomainIds;

    private Integer bizType;

    /**
     * status_change, time_reach
     */
    private String triggerType;

    /**
     * 状态变更相关
     * {"from":"处理中","to":"已关闭"}（仅 status_change 时有效）
     */
    private String triggerCondition;

    /**
     * 时间到达相关
     * 如 expect_resolve_time （仅 scheduled 时有效）
     */
    private String timeField;

    /**
     * 提前多少天提醒 （仅 scheduled 时有效）
     */
    private Integer remindDaysBefore;

    /**
     * "09:00" （仅 scheduled 时有效）
     */
    private String remindTime;

    /**
     * solver, custom, role...
     */
    private String receiverType;

    /**
     * ["user1", "user2"]
     */
    private String receiverIds;

    /**
     * send_notification
     */
    private String actionType;

    /**
     * { "channels": ["dingtalk", "site"], "template": "..." }
     */
    private String actionConfig;

    private Boolean isEnabled;

    private Boolean isOverdueNotify;
}
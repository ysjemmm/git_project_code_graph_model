package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/16 18:12
 */
@Getter
public enum MessageTitleEnum {
    // 业务需求状态变更通知标题
    BIZDEMAND_FEEDBACK("业务需求反馈通知"),
    BIZDEMAND_STATUS_CHANGE("业务需求进度变更通知"),
    BIZDEMAND_RECEIVE("您收到一条新的业务需求"),
    BIZDEMAND_INVALID("业务需求作废通知"),
    BIZDEMAND_MODIFY("您收到一条业务需求修改通知"),
    // 评论区@通知标题
    COMMENT("评论通知"),
    TEST_BILL("提测单消息通知"),
    TASK_DONE("任务完成通知"),
    // 线下BUG通知标题
    BUG_OFFLINE_ADD("您收到一条新的线下bug"),
    BUG_OFFLINE_CHECK("线下bug待验收通知"),
    BUG_OFFLINE_CHECK_FAIL("线下bug验收失败通知"),
    BUG_OFFLINE_DELAY_REPAIR("线下bug延期修复通知"),
    BUG_OFFLINE_NO_REPAIR("线下bug不用修复通知"),
    BUG_OFFLINE_TRANS("线下bug转交通知"),
    BUG_OFFLINE_REJECT("线下bug被拒绝通知"),
    BUG_OFFLINE_OPEN_AGAIN("线下bug重新打开通知"),
    BUG_ONLINE_REPAIR_FINISHED("线上bug修复完毕通知"),
    BUG_ONLINE_ONLINE("线上bug已上线通知"),
    BUG_ONLINE_SUBMIT("线上bug提交通知"),
    BUG_ONLINE_MODIFY_OPERATOR("线上bug修改经办人通知"),
    BUG_ONLINE_OPEN_AGAIN("线上bug重新打开通知"),
    BUG_ONLINE_NO_REPAIR("线上bug不用修复通知"),
    BUG_ONLINE_TRANSFER("线上bug转交通知"),
    BUG_ONLINE_REJECT("线上bug拒绝通知"),
    BUG_ONLINE_REPAIR_FAIL("线上bug修复失败通知"),
    BUG_ONLINE_LINK_BUG_PROCESS("关联的线上bug处理进度通知"),

    // 人天通知标题
    MAN_DAY_AUDIT("人天审核通知"),
    MAN_DAY_REJECT("人天驳回通知"),
    MAN_DAY_APPROVE("人天通过通知"),
    MAN_DAY_URGE("您收到一条催办消息"),

    // 改进措施
    IMPROVEMENT_MEASURE("您收到了一条故障改进事项"),

    CUSTOMDEMAND_RECEIVE("您收到一条新的客户需求"),

    PROJECT_ESTABLISH_DATE_CHANGE("立项预期上线时间变更通知"),

    TRACK_EVENT_APPROVAL("您有新的需要审批的埋点事件"),

    PROJECT_NODE_DELAY_UNINPUT("项目节点逾期未录入通知"),

    //项目验收
    PROJECT_ACCEPTANCE_START("项目验收发起通知"),

    PROJECT_ACCEPTANCE_REMIND("项目验收催办通知"),

    PROJECT_ACCEPTANCE_REVOKE("项目验收撤回通知"),

    PROJECT_ACCEPTANCE_ACCEPT("项目验收通过通知"),

    PROJECT_ACCEPTANCE_UNACCEPT("项目验收不通过通知"),
    ;

    private String text;

    MessageTitleEnum(String text) {
        this.text = text;
    }
}

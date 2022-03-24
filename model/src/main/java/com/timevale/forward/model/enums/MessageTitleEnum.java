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
    // 评论区@通知标题
    COMMENT("评论通知"),
    TEST_BILL("提测单消息通知"),
    TASK_DONE("任务完成通知"),
    // 线下BUG通知标题
    BUG_OFFLINE_ADD("您收到一条新线下bug"),
    BUG_OFFLINE_CHECK("线下bug待验收通知"),
    BUG_OFFLINE_CHECK_FAIL("线下bug验收失败通知"),
    BUG_OFFLINE_DELAY_REPAIR("线下bug延期修复通知"),
    BUG_OFFLINE_NO_REPAIR("线下bug不用修复通知"),
    BUG_OFFLINE_TRANS("线下bug转交通知"),
    BUG_OFFLINE_REJECT("线下bug被拒绝通知"),
    BUG_OFFLINE_OPEN_AGAIN("线下bug重新打开通知")
    ;

    private String text;

    MessageTitleEnum(String text){this.text = text;}
}

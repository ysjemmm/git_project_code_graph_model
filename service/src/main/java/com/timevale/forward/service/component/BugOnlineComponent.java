package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.model.enums.ButtonActionEnum;

import java.util.Collection;

public interface BugOnlineComponent {
    /**
     *
     * @param autoCloseLimitDay 多少天自动关闭
     */
    void autoCloseBugIfBeConfirm(int autoCloseLimitDay);

    /**
     * 自动钉钉通知关闭线上bug确认
     */
    void autoNoticeCloseBugIfBeConfirm(int autoCloseLimitDay);

    /**
     * 关联业务需求转需求
     */
    void attachToBizDemands(BugOnlineDO bugOnline, Collection<Long> bizDemandIds, ButtonActionEnum actionEnum);

    /**
     * 更新可以为空的字段
     *
     * @param bugOnlineDO 错误在线DO
     */
    void updateCanNull(BugOnlineDO bugOnlineDO);
}

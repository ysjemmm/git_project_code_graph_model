package com.timevale.forward.service.component;

public interface BugOnlineComponent {
    /**
     *
     * @param autoCloseLimitDay 多少天自动关闭
     */
    void autoCloseBugIfBeConfirm(int autoCloseLimitDay);

    /**
     * 自动钉钉通知关闭线上bug确认
     * @param autoCloseLimitDay
     */
    void autoNoticeCloseBugIfBeConfirm(int autoCloseLimitDay);

}

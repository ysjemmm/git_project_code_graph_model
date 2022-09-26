package com.timevale.forward.service.component;

public interface BugOnlineComponent {
    /**
     *
     * @param autoCloseLimitDay 多少天自动关闭
     */
    void autoCloseBugIfBeConfirm(int autoCloseLimitDay);

}

package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date 2022/2/25 19:17
 * @Author 望轩
 */
public class BugOfflineSelfTestPassMsgEvent extends MessageEvent {
    /**
     * 操作人，花名-真名
     */
    private String operator;
    /**
     * bug标题
     */
    private String bugName;
    /**
     * 接收人,用户id
     */
    private String receiver;

    public BugOfflineSelfTestPassMsgEvent(Object source, String operator, String bugName, String receiver) {
        super(source);
        this.operator = operator;
        this.bugName = bugName;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        String singleUrl = "www.baidu.com";
        String markdown = String.format("%s自测通过【线下bug】%s,请验收，可进入产研项目管理系统查看:%s", operator, bugName, singleUrl);
        List<String> receivers = new ArrayList<>();
        receivers.add(receiver);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title("线下bug待验收通知")
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
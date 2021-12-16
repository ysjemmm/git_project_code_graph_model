package com.timevale.forward.service.integration.erp.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author yuankai
 * @date 2021/12/16 15:52
 */
@Getter
@Builder
public class ActionCardMsg {
    /**
     * 接收人花名拼音
     */
    private List<String> receivers;
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String markdown;
    /**
     * 跳转连接文案
     */
    private String singleTitle;
    /**
     * 连接
     */
    private String singleUrl;
}

package com.timevale.forward.service.integration.erp.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * @author yuankai
 * @date 2021/12/16 15:54
 */
@Getter
@Builder
@ToString
public class MarkdownMsg {
    /**
     * 花名拼音
     */
    private List<String> receivers;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;
}

package com.timevale.forward.service.integration.erp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author jingchun
 * create on 8/4/2023
 **/
@Getter
@Setter
@Accessors(fluent = true)
public class AddDingTodoReq {


    /**
     * 标题
     */
    private String title;

    /**
     * 接收人ID
     */
    private String receiveId;

    /**
     * 待办URL
     */
    private String url;

    /**
     * 内容
     */
    private String content;

    /**
     * 截止时间
     */
    private Long dueTime;

}

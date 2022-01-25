package com.timevale.forward.service.integration.erp.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 钉钉待办消息
 *
 * @author xingyun
 * @date 2022/01/25 10:51
 */
@Getter
@Builder
public class CreateTodoTaskMsg {
    /**
     * 待办标题
     */
    private String title;

    /**
     * 待办接收人unionId
     */
    private List<String> executorIds;
    /**
     * 待办操作人unionId
     */
    private String unionId;

    /**
     * 待办截止时间
     */
    private Long dueTime;
}

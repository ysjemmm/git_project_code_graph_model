package com.timevale.forward.service.integration.erp.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 钉钉待办消息
 *
 * @author xingyun
 * @date 2022/01/25 10:51
 */
@Builder
@Data
public class UpdateTodoTaskMsg {
    /**
     * 待办id
     */
    private String recordId;

    /**
     * 标题
     */
    private String title;

    /**
     * 待办执行人unionId
     */
    private List<String> executorIds;

    /**
     * 待办参与人unionId
     */
    private List<String> participantIds;
    /**
     * 待办操作人unionId
     */
    private String unionId;

    /**
     * 待办是否完成
     */
    private Boolean done;

    /**
     * 待办截止时间
     */
    private Long dueTime;
}

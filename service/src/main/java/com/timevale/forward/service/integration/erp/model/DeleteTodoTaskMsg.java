package com.timevale.forward.service.integration.erp.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 钉钉待办消息
 *
 * @author xingyun
 * @date 2022/01/25 10:51
 */
@Getter
@Builder
public class DeleteTodoTaskMsg {
    /**
     * 待办id
     */
    private String recordId;

    /**
     * 待办操作人unionId
     */
    private String unionId;

}

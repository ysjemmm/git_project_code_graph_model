package com.timevale.forward.service.mq.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author by YangXu
 * @date 2023/05/09 16:38
 */
@Getter
@Setter
public class PersonStatusDTO {
    /**
     * 花名拼音
     */
    private String account;
    /**
     * 状态：0在职，1离职
     */
    private Integer status;

    public boolean isResign() {
        return status == 1;
    }
}


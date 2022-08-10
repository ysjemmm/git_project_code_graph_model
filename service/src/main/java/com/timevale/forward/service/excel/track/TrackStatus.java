package com.timevale.forward.service.excel.track;

import lombok.Data;

/**
 * @author by YangXu
 * @date 2022/08/10 14:27
 */
@Data
public class TrackStatus {
    /**
     * 是否取消导入
     */
    private Boolean cancel = false;
}

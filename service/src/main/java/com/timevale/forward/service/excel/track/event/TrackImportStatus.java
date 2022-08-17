package com.timevale.forward.service.excel.track.event;

import com.timevale.mandarin.common.result.ToString;
import lombok.Data;

/**
 * @author by YangXu
 * @date 2022/08/10 14:27
 */
@Data
public class TrackImportStatus extends ToString {

    /**
     * 导入进度
     */
    private Integer progress = 0;

    /**
     * 是否取消导入
     */
    private Boolean cancel = false;
}

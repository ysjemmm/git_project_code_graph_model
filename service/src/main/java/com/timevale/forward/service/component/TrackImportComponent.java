package com.timevale.forward.service.component;

import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.service.excel.track.TrackImportStatus;

/**
 * @author by YangXu
 * @date 2022/08/12 15:55
 */
public interface TrackImportComponent {

    /**
     * 获得当前导入状态
     *
     * @return {@link TrackImportStatus}
     */
    TrackImportStatus getStatus();

    /**
     * 配置当前导入状态
     *
     * @param status 状态
     */
    void setStatus(TrackImportStatus status);

    void importEvent(TrackImportReq trackImportReq);
}

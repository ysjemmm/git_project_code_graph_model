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
    int getProgress();

    /**
     * 更新进展
     *
     * @param progress 进展
     */
    void updateProgress(int progress);

    /**
     * 标记取消
     */
    void cancelTag();

    /**
     * 删除状态
     */
    void deleteStatus();

    void importEvent(TrackImportReq trackImportReq);
}

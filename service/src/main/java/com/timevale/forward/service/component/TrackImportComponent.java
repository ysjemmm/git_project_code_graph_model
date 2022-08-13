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
    Integer getProgress(String userId);

    /**
     * 更新进展
     *
     * @param progress 进展
     */
    void setProgress(Integer progress, String userId);

    /**
     * 取消导入状态
     */
    void setCancelTag(String userId);

    /**
     * 得到取消标签
     */
    Boolean getCancelTag(String userId);

    /**
     * 设置导入结果
     */
    void setImportResult(Integer result, String userId);

    /**
     * 得到导入结果
     */
    Integer getImportResult(String userId);

    /**
     * 删除状态
     */
    void deleteStatus(String userId);

    void importEvent(TrackImportReq trackImportReq, String userId);

    void importEnd(String userId);
}

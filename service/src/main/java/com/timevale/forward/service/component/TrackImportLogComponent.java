package com.timevale.forward.service.component;

import com.timevale.forward.service.utils.envoy.UserInfo;

/**
 * @author by YangXu
 * @date 2022/08/12 15:55
 */
public interface TrackImportLogComponent {

    void failLog(String importFileId, int importCount, int importFailCount, UserInfo userInfo);

    void allFailLog(String importFileId, UserInfo userInfo);

    void successLog(String importFileId,int importCount, UserInfo userInfo);
}

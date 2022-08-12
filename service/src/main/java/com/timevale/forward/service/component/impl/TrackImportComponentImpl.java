package com.timevale.forward.service.component.impl;

import com.timevale.forward.service.component.TrackImportComponent;
import com.timevale.forward.service.excel.track.TrackImportStatus;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.framework.tedis.util.TedisUtil;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2022/08/12 15:55
 */
@Component
public class TrackImportComponentImpl implements TrackImportComponent {

    private static final String TRACK_IMPORT_STATUS = "forward:track:import:status:";

    @Override
    public TrackImportStatus getStatus() {


        return null;
    }

    @Override
    public void setStatus(TrackImportStatus status) {
        String statusKey = TRACK_IMPORT_STATUS + LocalSessionUtils.getUserInfo().getId();

        TedisUtil.set(statusKey, status);

    }

}

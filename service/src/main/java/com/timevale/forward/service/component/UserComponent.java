package com.timevale.forward.service.component;

import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/13
 */
@Component
@RequiredArgsConstructor
public class UserComponent {
    private final InnerUserPersonClient personClient;

    /**
     * 查询是否PMO或者PMO上级
     */
    public boolean isPMO() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<BaseInfoResponse> users =
                personClient.getAllMyStaffWithSelfInfo(userInfo.getId(), false);
        for (BaseInfoResponse user : users) {
            if (CommonConstant.PMO.equalsIgnoreCase(user.getJobClassification())) {
                return true;
            }
        }
        return false;
    }


}

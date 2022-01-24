package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.HomePageDataIndicatorDTO;
import com.timevale.forward.service.component.HomePageDataIndicatorComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 09:58
 */
@Slf4j
@Component
public class HomePageDataIndicatorComponentImpl extends BaseDistributeClientImpl<HomePageDataIndicatorDTO> implements HomePageDataIndicatorComponent {
    // @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public HomePageDataIndicatorDTO getDataIndicator() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());

        ParamHelper paramHelper = ParamHelper.newInstance()
                .offset(0)
                .page(1)
                .in("user_id", allMyStaffWithSelf);

        DistributePageQueryVO params = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfig.getProjectOnlineLately())
                .build();

        List<HomePageDataIndicatorDTO> list = doGet(params);

        return list.get(0);
    }
}

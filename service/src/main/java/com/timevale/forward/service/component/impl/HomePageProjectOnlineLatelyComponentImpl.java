package com.timevale.forward.service.component.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dto.HomePageDataIndicatorDTO;
import com.timevale.forward.dal.dto.HomePageProjectOnlineLatelyDTO;
import com.timevale.forward.service.component.HomePageProjectOnlineLatelyComponent;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.facade.api.result.HomePageProjectOnlineLatelyVO;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 10:05
 */
@Slf4j
@Component
public class HomePageProjectOnlineLatelyComponentImpl extends BaseDistributeClientImpl<HomePageProjectOnlineLatelyDTO> implements HomePageProjectOnlineLatelyComponent {

    // @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public List<HomePageProjectOnlineLatelyDTO> getProjectOnlineLately(HomePageProjectOnlineLatelyQueryList homePageProjectOnlineLatelyQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        ParamHelper paramHelper = ParamHelper.newInstance()
                .offset(0)
                .page(1)
                .equals("user_id", userInfo.getId());

        DistributePageQueryVO params = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfig.getDataIndicator())
                .build();

        return doGet(params);
    }
}

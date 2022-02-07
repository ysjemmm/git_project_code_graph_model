package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.HomePageProjectOnlineLatelyDTO;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.service.component.HomePageProjectOnlineLatelyComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.model.base.PageResult;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
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

    @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public PageResult<HomePageProjectOnlineLatelyDTO> getProjectOnlineLately(HomePageProjectOnlineLatelyQueryList homePageProjectOnlineLatelyQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());

        ParamHelper queryParamHelper = ParamHelper.newInstance()
                .offset((homePageProjectOnlineLatelyQueryList.getPageNum() - 1) * homePageProjectOnlineLatelyQueryList.getPageSize())
                .page(homePageProjectOnlineLatelyQueryList.getPageSize())
                .in("user_id", allMyStaffWithSelf);
        DistributePageQueryVO queryParams = DistributePageQueryVO.builder()
                .params(queryParamHelper.params())
                .distributeConfigVO(distributeConfig.getProjectOnlineLately())
                .build();

        ParamHelper countParamHelper = ParamHelper.newInstance()
                .in("user_id", allMyStaffWithSelf);
        DistributePageQueryVO countParams = DistributePageQueryVO.builder()
                .params(countParamHelper.params())
                .distributeConfigVO(distributeConfig.getProjectOnlineLatelyCount())
                .build();
        return doPage(queryParams, countParams);
    }
}

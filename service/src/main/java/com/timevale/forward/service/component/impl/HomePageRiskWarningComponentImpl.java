package com.timevale.forward.service.component.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dto.HomePageDataIndicatorDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.service.component.HomePageRiskWarningComponent;
import com.timevale.forward.facade.api.result.HomePageRiskWarningVO;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 10:10
 */
@Slf4j
@Component
public class HomePageRiskWarningComponentImpl extends BaseDistributeClientImpl<HomePageRiskWarningDTO> implements HomePageRiskWarningComponent {

    // @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public List<HomePageRiskWarningDTO> getRiskWarning() {
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

        List<HomePageRiskWarningDTO> list = doGet(params);

        return list;
    }
}

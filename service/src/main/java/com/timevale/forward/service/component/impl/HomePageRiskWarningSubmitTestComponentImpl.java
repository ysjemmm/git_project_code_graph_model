package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.HomePageRiskWarningSubmitTestDTO;
import com.timevale.forward.model.enums.UserTypeEnum;
import com.timevale.forward.service.component.HomePageRiskWarningSubmitTestComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.config.DistributeConfigVO;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
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
 * @date 2022/01/24 10:10
 */
@Slf4j
@Component
public class HomePageRiskWarningSubmitTestComponentImpl extends BaseDistributeClientImpl<HomePageRiskWarningSubmitTestDTO> implements HomePageRiskWarningSubmitTestComponent {

    @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public List<HomePageRiskWarningSubmitTestDTO> getRiskWarningSubmitTest() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);

        ParamHelper paramHelper = ParamHelper.newInstance()
                .offset(0)
                .page(Integer.MAX_VALUE)
                .in("user_id", allMyStaffWithSelf);

        DistributePageQueryVO params = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfig.getRiskWarningSubmitTest())
                .build();

        return doGet(params);
    }
}

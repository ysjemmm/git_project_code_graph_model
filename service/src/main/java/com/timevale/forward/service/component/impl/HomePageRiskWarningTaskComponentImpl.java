package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.HomePageRiskWarningTaskDTO;
import com.timevale.forward.facade.api.request.HomePageBaseReq;
import com.timevale.forward.model.enums.HomePageTabEnum;
import com.timevale.forward.service.component.HomePageRiskWarningTaskComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
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
public class HomePageRiskWarningTaskComponentImpl extends BaseDistributeClientImpl<HomePageRiskWarningTaskDTO> implements HomePageRiskWarningTaskComponent {

    @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public List<HomePageRiskWarningTaskDTO> getRiskWarningTask(HomePageBaseReq homePageBaseReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<String> allMyStaffWithSelf;
        if(HomePageTabEnum.INDIVIDUAL.getCode().equals(homePageBaseReq.getTabType())){
            allMyStaffWithSelf = Lists.newArrayList(userInfo.getId());
        }else {
            allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
        }

        ParamHelper paramHelper = ParamHelper.newInstance()
                .offset(0)
                .page(Integer.MAX_VALUE)
                .in("user_id", allMyStaffWithSelf);

        DistributePageQueryVO params = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfig.getRiskWarningTask())
                .build();

        return doGet(params);
    }
}

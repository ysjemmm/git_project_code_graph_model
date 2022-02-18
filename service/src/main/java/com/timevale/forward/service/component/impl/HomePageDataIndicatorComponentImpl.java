package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.HomePageDataIndicatorDTO;
import com.timevale.forward.model.enums.UserTypeEnum;
import com.timevale.forward.service.component.HomePageDataIndicatorComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.config.DistributeConfigVO;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
    @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public HomePageDataIndicatorDTO getDataIndicator() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);

        ParamHelper paramHelper = ParamHelper.newInstance()
                .equals("user_id", userInfo.getId());

        // 根据用户类型访问不同接口
        DistributeConfigVO distributeConfigVO;
        if(allMyStaffWithSelf.size() == 1){
            distributeConfigVO = distributeConfig.getDataIndicatorCommon();
        }else{
            distributeConfigVO = distributeConfig.getDataIndicatorLeader();
        }

        DistributePageQueryVO params = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfigVO)
                .build();
        List<HomePageDataIndicatorDTO> homePageDataIndicatorDTOList = doGet(params);

        // 验空
        if(CollectionUtils.isEmpty(homePageDataIndicatorDTOList)){
            return new HomePageDataIndicatorDTO();
        }

        return homePageDataIndicatorDTOList.get(0);
    }
}

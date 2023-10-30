package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.HomePageProjectBoardDTO;
import com.timevale.forward.service.component.HomePageProjectBoardComponent;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 10:09
 */
@Slf4j
@Component
public class HomePageProjectBoardComponentImpl extends BaseDistributeClientImpl<HomePageProjectBoardDTO> implements HomePageProjectBoardComponent {
    @Resource
    private DistributeConfig distributeConfig;

    @Override
    public List<HomePageProjectBoardDTO> getProjectBoard(List<String> userIdList) {
        ParamHelper paramHelper = ParamHelper.newInstance()
                .offset(0)
                .page(Integer.MAX_VALUE)
                .in("user_id", userIdList);

        DistributePageQueryVO params = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfig.getProjectBoard())
                .build();
        return doGet(params);
    }
}

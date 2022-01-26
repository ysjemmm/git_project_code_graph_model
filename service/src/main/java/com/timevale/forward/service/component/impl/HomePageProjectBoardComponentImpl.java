package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.HomePageProjectBoardDTO;
import com.timevale.forward.facade.api.result.HomePageProjectBoardVO;
import com.timevale.forward.model.enums.UserTypeEnum;
import com.timevale.forward.service.component.HomePageProjectBoardComponent;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.config.DistributeConfigVO;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
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
    public List<HomePageProjectBoardDTO> getProjectBoard(String userType, List<String> userIdList) {
        ParamHelper paramHelper = ParamHelper.newInstance()
                .offset(0)
                .page(Integer.MAX_VALUE)
                .in("user_id", Lists.emptyList());

        // 根据用户类型访问不同接口
        DistributeConfigVO distributeConfigVO;
        if(userType.equals(UserTypeEnum.PD.toString())){
            distributeConfigVO = distributeConfig.getProjectBoardPD();
        }else if(userType.equals(UserTypeEnum.RD.toString())){
            distributeConfigVO = distributeConfig.getProjectBoardRD();
        }else{
            distributeConfigVO = distributeConfig.getProjectBoardQA();
        }
        DistributePageQueryVO params = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfigVO)
                .build();
        return doGet(params);
    }
}

package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.facade.api.client.HomePageService;
import com.timevale.forward.facade.api.query.HomePageProjectBoardQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.UserType;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/01/11 17:43
 */
@Slf4j
@RestService
public class HomePageServiceImpl implements HomePageService {

    @Resource
    HomePageDataIndicatorComponent homePageDataIndicatorComponent;

    @Resource
    HomePageProjectOnlineLatelyComponent homePageProjectOnlineLatelyComponent;

    @Resource
    HomePageProjectBoardComponent homePageProjectBoardComponent;

    @Resource
    HomePageRiskWarningComponent homePageRiskWarningComponent;

    @Resource
    HomePageTodoCardComponent homePageTodoCardComponent;

    @Resource
    InnerUserPersonClient innerUserPersonClient;

    @Resource
    ProjectMapper projectMapper;

    @Resource
    BizDemandMapper bizDemandMapper;

    @Override
    public BaseResult<HomePageDataIndicatorVO> getDataIndicator() {
        HomePageDataIndicatorVO dataIndicatorVO=new HomePageDataIndicatorVO();
        return BaseResult.success(dataIndicatorVO);
    }

    @Override
    public BaseResult<HomePageTodoCardVO> getTodoCard(String userType) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        HomePageTodoCardVO todoCardVO = new HomePageTodoCardVO();

        // 获取我及所有下属
        List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());

        // 进行中的项目
        List<ProjectListDO> projectListDOList = projectMapper.list(ProjectListCondition.builder()
                .teamMembers(allMyStaffWithSelf)
                .build());
        int projectCount = Math.toIntExact(projectListDOList.stream().filter(e -> ProjectStatusEnum.ongoing(e.getStatus())).count());
        todoCardVO.setProjectCount(projectCount);

        // 如果为产品则添加待处理业务需求，否则添加待处理任务
        if(userType.equals(UserType.PD.toString())){
            // 待处理业务
            List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(BizDemandListCondition.builder()
                    .receiveManIdList(allMyStaffWithSelf)
                    .build());
            int bizDemandCount = Math.toIntExact(bizDemandListDOList.stream().filter(e -> e.getStatus().equals(BizDemandStatusEnum.EVALUATE.getCode())).count());
            todoCardVO.setBizDemandCount(bizDemandCount);
        }else{
            // 待完成的任务
        }
        return BaseResult.success(todoCardVO);
    }

    @Override
    public BaseResult<PageQueryResult<HomePageProjectOnlineLatelyVO>> getProjectOnlineLately() {
        PageQueryResult<HomePageProjectOnlineLatelyVO> pageQueryResult = new PageQueryResult<>();
//        ProjectOnlineLatelyVO projectOnlineLatelyVO=new ProjectOnlineLatelyVO();
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<HomePageRiskWarningVO> getRiskWarning() {
        HomePageRiskWarningVO riskWarningVO=new HomePageRiskWarningVO();
        return BaseResult.success(riskWarningVO);
    }

    @Override
    public BaseResult<HomePageProjectBoardVO> getProjectBoard(HomePageProjectBoardQueryList boardQueryList) {
        HomePageProjectBoardVO projectBoardVO=new HomePageProjectBoardVO();
        return BaseResult.success(projectBoardVO);
    }
}

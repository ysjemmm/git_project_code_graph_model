package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dto.HomePageDataIndicatorDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.facade.api.client.HomePageService;
import com.timevale.forward.facade.api.query.HomePageProjectBoardQueryList;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.UserTypeEnum;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.copy.HomePageDataIndicatorCopier;
import com.timevale.forward.service.copy.HomePageRiskWarningCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
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
    public BaseResult<HomePageDataIndicatorVO> getDataIndicator(String userType) {
        HomePageDataIndicatorDTO dataIndicator = homePageDataIndicatorComponent.getDataIndicator(userType);
        return BaseResult.success(HomePageDataIndicatorCopier.INSTANCE.convert(dataIndicator));
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
        if(userType.equals(UserTypeEnum.PD.toString())){
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
    public BaseResult<List<HomePageRiskWarningVO>> getRiskWarning(String userType) {
        List<HomePageRiskWarningDTO> riskWarningDTOList = homePageRiskWarningComponent.getRiskWarning(userType);

        // 按项目id分组
        Map<Long, List<HomePageRiskWarningDTO>> riskWarningGroup = riskWarningDTOList.stream().collect(Collectors.groupingBy(HomePageRiskWarningDTO::getProjectId));

        // 转换填充
        List<HomePageRiskWarningVO> riskWarningVOList = Lists.newArrayList();
        for (List<HomePageRiskWarningDTO> dtoList : riskWarningGroup.values()) {
            HomePageRiskWarningVO riskWarningVO = new HomePageRiskWarningVO();
            riskWarningVO.setProjectId(dtoList.get(0).getProjectId());
            riskWarningVO.setProjectName(dtoList.get(0).getProjectName());
            riskWarningVO.setHomePageProjectNodeVO(HomePageRiskWarningCopier.INSTANCE.convert(dtoList));
            riskWarningVOList.add(riskWarningVO);
        }

        return BaseResult.success(riskWarningVOList);
    }

    @Override
    public BaseResult<HomePageProjectBoardVO> getProjectBoard(HomePageProjectBoardQueryList boardQueryList) {
        HomePageProjectBoardVO projectBoardVO=new HomePageProjectBoardVO();
        return BaseResult.success(projectBoardVO);
    }
}

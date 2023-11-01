package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.facade.api.request.HomePageBaseReq;
import com.timevale.forward.model.enums.HomePageTabEnum;
import com.timevale.forward.model.enums.JobFunctionEnum;
import com.timevale.forward.model.enums.UserTypeEnum;
import com.timevale.forward.service.component.HomePageRiskWarningComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.util.ParamHelper;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/01/24 10:10
 */
@Slf4j
@Component
public class HomePageRiskWarningComponentImpl extends BaseDistributeClientImpl<HomePageRiskWarningDTO> implements HomePageRiskWarningComponent {

    @Resource
    private DistributeConfig distributeConfig;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public List<HomePageRiskWarningDTO> getRiskWarning(HomePageBaseReq homePageBaseReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<BaseInfoResponse> allMyStaffInfoWithSelf;
        if(HomePageTabEnum.INDIVIDUAL.getCode().equals(homePageBaseReq.getTabType())){
            allMyStaffInfoWithSelf = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));
        }else{
            allMyStaffInfoWithSelf = innerUserPersonClient.getAllMyStaffWithSelfInfo(userInfo.getId(), true);
        }

        // 区分身份
        List<BaseInfoResponse> PDList = allMyStaffInfoWithSelf.stream()
                .filter(e -> !UserTypeEnum.QA.equals(JobFunctionEnum.getType(e.getJobFunction()))
                        && !UserTypeEnum.RD.equals(JobFunctionEnum.getType(e.getJobFunction())))
                .collect(Collectors.toList());
        List<BaseInfoResponse> QAList = allMyStaffInfoWithSelf.stream()
                .filter(e -> UserTypeEnum.QA.equals(JobFunctionEnum.getType(e.getJobFunction())))
                .collect(Collectors.toList());
        List<BaseInfoResponse> RDList = allMyStaffInfoWithSelf.stream()
                .filter(e -> UserTypeEnum.RD.equals(JobFunctionEnum.getType(e.getJobFunction())))
                .collect(Collectors.toList());

        List<String> PDNameList = PDList.stream().map(BaseInfoResponse::getAccount).collect(Collectors.toList());
        List<String> QANameList = QAList.stream().map(BaseInfoResponse::getAccount).collect(Collectors.toList());
        List<String> RDNameList = RDList.stream().map(BaseInfoResponse::getAccount).collect(Collectors.toList());

        Set<HomePageRiskWarningDTO> result = new HashSet<>();

        // 参数配置
        if(!CollectionUtils.isEmpty(PDNameList)){
            ParamHelper paramHelper = ParamHelper.newInstance()
                    .offset(0)
                    .page(Integer.MAX_VALUE)
                    .in("user_id", PDNameList);
            DistributePageQueryVO params = DistributePageQueryVO.builder()
                    .params(paramHelper.params())
                    .distributeConfigVO(distributeConfig.getRiskWarningPD())
                    .build();
            List<HomePageRiskWarningDTO> resultPD = doGet(params);
            result.addAll(resultPD);
            log.info("[getRiskWarning]PD节点风险预警{}",resultPD);
        }
        if(!CollectionUtils.isEmpty(QANameList)){
            ParamHelper paramHelper = ParamHelper.newInstance()
                    .offset(0)
                    .page(Integer.MAX_VALUE)
                    .in("user_id", QANameList);
            DistributePageQueryVO params = DistributePageQueryVO.builder()
                    .params(paramHelper.params())
                    .distributeConfigVO(distributeConfig.getRiskWarningQA())
                    .build();
            List<HomePageRiskWarningDTO> resultQA = doGet(params);
            result.addAll(resultQA);
            log.info("[getRiskWarning]QA节点风险预警{}",resultQA);
        }
        if(!CollectionUtils.isEmpty(RDNameList)){
            ParamHelper paramHelper = ParamHelper.newInstance()
                    .offset(0)
                    .page(Integer.MAX_VALUE)
                    .in("user_id", RDNameList);
            DistributePageQueryVO params = DistributePageQueryVO.builder()
                    .params(paramHelper.params())
                    .distributeConfigVO(distributeConfig.getRiskWarningRD())
                    .build();
            List<HomePageRiskWarningDTO> resultRD = doGet(params);
            result.addAll(resultRD);
            log.info("[getRiskWarning]RD节点风险预警{}",resultRD);
        }

        log.info("[getRiskWarning]节点风险预警{}",result);
        return new ArrayList<>(result);
    }

    @Override
    public List<HomePageRiskWarningDTO> getRiskWarningAll() {
        Set<HomePageRiskWarningDTO> resultSet = new HashSet<>();

        ParamHelper paramHelper = ParamHelper.newInstance()
                .offset(0)
                .page(Integer.MAX_VALUE);

        DistributePageQueryVO paramPD = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfig.getRiskWarningPD())
                .build();
        DistributePageQueryVO paramQA = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfig.getRiskWarningQA())
                .build();
        DistributePageQueryVO paramRD = DistributePageQueryVO.builder()
                .params(paramHelper.params())
                .distributeConfigVO(distributeConfig.getRiskWarningRD())
                .build();

        resultSet.addAll(doGet(paramPD));
        resultSet.addAll(doGet(paramQA));
        resultSet.addAll(doGet(paramRD));

        // 去重返回
        return new ArrayList<>(resultSet);
    }
}

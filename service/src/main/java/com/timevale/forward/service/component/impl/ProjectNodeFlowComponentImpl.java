package com.timevale.forward.service.component.impl;

import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.dal.dao.ProjectNodeFlowMapper;
import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.model.enums.EnvEnum;
import com.timevale.forward.model.enums.PlatformTypeEnum;
import com.timevale.forward.model.enums.TrackPropTypeEnum;
import com.timevale.forward.service.component.ProjectNodeFlowComponent;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProjectNodeFlowComponentImpl implements ProjectNodeFlowComponent {

    @Resource
    private ProjectNodeFlowMapper projectNodeFlowMapper;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Resource
    private EpeiusClient epeiusClient;

    @Override
    public void add(ProjectNodeFlowDO projectNodeFlowDO) {
        projectNodeFlowMapper.insert(projectNodeFlowDO);
    }

    @Override
    public void process(Date oldPlanEndDate, Date planEndDate, ProjectNodeFlowDO projectNodeFlowDO) {
        oldPlanEndDate = DateUtil.getEndOfDay(oldPlanEndDate);
        planEndDate = DateUtil.getEndOfDay(planEndDate);
        if (projectNodeFlowDO != null && oldPlanEndDate.before(planEndDate)) {
            Long seconds = elapsedTimeClient.getElapsedTime(oldPlanEndDate, planEndDate);
        }

    }

    private String startFlow(TrackEventAddReq trackEventAddReq) {
        StartProcessRequest start = new StartProcessRequest();
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullCnName", trackEventAddReq.getFullCnName());
        variables.put("egName", trackEventAddReq.getEgName());
        variables.put("platform", StringUtils.join(PlatformTypeEnum.getTextByCode(trackEventAddReq.getPlatforms()), ","));
        variables.put("touchMoment", trackEventAddReq.getTouchMoment());
        variables.put("env", StringUtils.join(EnvEnum.getTextByCode(trackEventAddReq.getEnvs()), ","));
        variables.put("trackEventName", trackEventAddReq.getFullCnName());


        List<Integer> types = Lists.newArrayList(TrackPropTypeEnum.DEFAULT.getCode());
        TrackPropListCondition c = TrackPropListCondition.builder().types(types).build();


        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_trackEventReview");
        start.setStartAccountId(userInfo.getId());
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        String id = epeiusClient.start(start);
        return id;
    }
}

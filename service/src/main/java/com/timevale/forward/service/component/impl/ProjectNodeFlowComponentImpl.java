package com.timevale.forward.service.component.impl;

import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.ProjectNodeFlowMapper;
import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import com.timevale.forward.model.enums.FlowStageEnum;
import com.timevale.forward.model.enums.FlowStatusEnum;
import com.timevale.forward.service.component.ProjectNodeFlowComponent;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
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
    public void process(ProjectNodeFlowDO projectNodeFlowDO) {
        log.info("节点审批流程发起,参数:{}", projectNodeFlowDO);
        if(projectNodeFlowDO!=null){
            Date oldPlanEndDate = DateUtil.getEndOfDay(projectNodeFlowDO.getPublishDate());
            Date planEndDate = DateUtil.getEndOfDay(projectNodeFlowDO.getChangePublishDate());
            if (oldPlanEndDate.before(planEndDate)) {
//                Long seconds = elapsedTimeClient.getElapsedTime(oldPlanEndDate, planEndDate);
//                BigDecimal elapsedTime = new BigDecimal(seconds.toString());
//                elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
//                if(BigDecimal.ZERO.equals(elapsedTime)){
//                    return;
//                }
                projectNodeFlowDO.setDelayDay(BigDecimal.valueOf(1));
                projectNodeFlowDO.setFlowId(startFlow(projectNodeFlowDO));
                projectNodeFlowDO.setStatus(FlowStatusEnum.AUDITING.getCode());
                projectNodeFlowMapper.insert(projectNodeFlowDO);
            }
        }
    }

    private String startFlow(ProjectNodeFlowDO projectNodeFlowDO) {
        StartProcessRequest start = new StartProcessRequest();
        Map<String, Object> variables = new HashMap<>();
        variables.put("reason", projectNodeFlowDO.getReason());
        variables.put("pd", projectNodeFlowDO.getPd());
        variables.put("biz", projectNodeFlowDO.getBiz());
        variables.put("po", projectNodeFlowDO.getPo());
        variables.put("d", projectNodeFlowDO.getD());
        variables.put("publishDate", projectNodeFlowDO.getPublishDate());
        variables.put("changePublishDate", projectNodeFlowDO.getChangePublishDate());
        variables.put("delayDay", projectNodeFlowDO.getDelayDay());
        if(StringUtils.isEmpty(projectNodeFlowDO.getPd())&&StringUtils.isEmpty(projectNodeFlowDO.getBiz())){
            projectNodeFlowDO.setStage(FlowStageEnum.SECOND.getCode());
        }else{
            projectNodeFlowDO.setStage(FlowStageEnum.FIRST.getCode());
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_projectNodeReview");
        start.setStartAccountId(userInfo.getId());
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
//        String id = epeiusClient.start(start);
        return "";
    }
}

package com.timevale.forward.service.job;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.ProjectRiskRecordDO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.observer.event.ProjectNodeDelayUnInputMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * create on 2022/6/27
 */
@Slf4j
@JobHandler(value = "ProjectNodeDelayUnInputNotifyJob")
public class ProjectNodeDelayUnInputNotifyJob extends IJobHandler {

    @Value("${notify.time:10:00}")
    private String notifyTime;

    @Resource
    private ProjectRiskMapper projectRiskMapper;

    @Resource
    private ProjectRiskRecordMapper projectRiskRecordMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Override
    public ReturnT<String> execute(String s) {
        log.info("项目节点逾期未录入,任务开始");
        //未处理的逾期未录入风险
        List<Long> projectIds = projectRiskMapper.selectByStatusType(ProjectRiskStatusEnum.PENDING.getCode(), ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode());

        List<ProjectDO> projectDOList = projectMapper.getByIds(projectIds);
        List<ProjectDO> filter = projectDOList.stream().filter(a -> !ProjectStatusEnum.terminated(a.getStatus())).collect(Collectors.toList());
        List<Long> filterIds = filter.stream().map(ProjectDO::getId).collect(Collectors.toList());
        Map<Long, ProjectDO> projectMap = filter.stream().collect(Collectors.toMap(ProjectDO::getId, b -> b, (v1, v2) -> v2));

        Map<Long, List<PersonDO>> pdMap = personMapper.get(filterIds, PersonTypeEnum.PROJECT_PD.getCode())
                .stream().collect(Collectors.groupingBy(PersonDO::getMainId));
        //待处理的节点
        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.selectByProjectIdList(filterIds);
        List<ProjectNodeDO> filterProjectNodes = projectNodeDOList.stream().filter(a -> a.getPlanDate() != null && a.getActualDate() == null).collect(Collectors.toList());
        //已经处理过的记录
        List<ProjectRiskRecordDO> projectRiskRecordDOList = projectRiskRecordMapper.get(null,ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode());
        Map<String, ProjectRiskRecordDO> riskRecordMap = projectRiskRecordDOList.stream()
                .collect(Collectors.toMap(a -> a.getMainId() + "-" + a.getName() + "-" + a.getReceiveManId(), b -> b, (v1, v2) -> v2));
        List<ProjectRiskRecordDO> result = new ArrayList<>();
        for (ProjectNodeDO nodeDO : filterProjectNodes) {
            if (!projectMap.containsKey(nodeDO.getProjectId())) {
                continue;
            }
            ProjectDO projectDO = projectMap.get(nodeDO.getProjectId());
            Integer code = ProjectNodeEnum.getCodeByName(nodeDO.getName());
            if (code < 30) {
                //需求规划阶段,消息接收人找pd
                if (!pdMap.containsKey(nodeDO.getProjectId())) {
                    continue;
                }
                for (PersonDO personDO : pdMap.get(nodeDO.getProjectId())) {
                    String key = nodeDO.getProjectId() + "-" + nodeDO.getName() + "-" + personDO.getUserId();
                    if (!riskRecordMap.containsKey(key)) {
                        result.add(createProjectRiskRecordDO(nodeDO.getProjectId(), nodeDO.getName(), personDO.getUserName(), personDO.getUserId()));
                        send(nodeDO.getProjectId(),nodeDO.getName(),nodeDO.getPlanDate(),personDO.getUserId());
                    }
                }

            } else {
                //其他阶段,消息接收人找pm
                String key = nodeDO.getProjectId() + "-" + nodeDO.getName() + "-" + projectDO.getPmId();
                if (!riskRecordMap.containsKey(key)) {
                    result.add(createProjectRiskRecordDO(nodeDO.getProjectId(), nodeDO.getName(), projectDO.getPmName(), projectDO.getPmId()));
                    send(nodeDO.getProjectId(),nodeDO.getName(),nodeDO.getPlanDate(),projectDO.getPmId());
                }

            }
        }
        projectRiskRecordMapper.batchInsert(result);
        log.info("项目节点逾期未录入,任务结束,共计: {}条", result.size());
        return ReturnT.SUCCESS;
    }

    private ProjectRiskRecordDO createProjectRiskRecordDO(Long projectId, String name, String receiveMan, String receiveManId) {
        ProjectRiskRecordDO riskRecordDO = new ProjectRiskRecordDO();
        riskRecordDO.setName(name);
        riskRecordDO.setMainId(projectId);
        riskRecordDO.setType(ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode());
        riskRecordDO.setReceiveMan(receiveMan);
        riskRecordDO.setReceiveManId(receiveManId);
        return riskRecordDO;
    }

    private void send(Long projectId,String name, Date planDate, String receiveManId) {
        messageEventPublisher.publish(new ProjectNodeDelayUnInputMsgEvent(
                this,
                projectId,
                receiveManId,
                name,
                DateUtil.parseToString(planDate, DateStyle.YYYY_MM_DD)
        ));
    }
}

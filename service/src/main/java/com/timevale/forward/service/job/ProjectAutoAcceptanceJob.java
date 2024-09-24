package com.timevale.forward.service.job;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.condition.ProjectAcceptanceListCondition;
import com.timevale.forward.dal.dao.ProjectAcceptanceMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.ProjectAcceptanceDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.ProjectAcceptanceService;
import com.timevale.forward.facade.api.request.ProjectAcceptanceModifyReq;
import com.timevale.forward.model.enums.ForwardFlowStatusEnum;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@JobHandler("projectAutoAcceptanceJob")
public class ProjectAutoAcceptanceJob extends IJobHandler {
    private final ProjectMapper projectMapper;
    private final ProjectAcceptanceMapper projectAcceptanceMapper;
    private final ProjectAcceptanceService projectAcceptanceService;

    @Value("${project.autoAcceptanceDay:3}")
    private int autoAcceptanceDay;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        Date createDateEnd = DateUtil.addDay(DateUtil.getStartOfDay(new Date()), -autoAcceptanceDay);
        List<Integer> status = Collections.singletonList(ForwardFlowStatusEnum.AUDITING.getCode());
        ProjectAcceptanceListCondition condition = ProjectAcceptanceListCondition.builder()
                .status(status)
                .createDateEnd(createDateEnd)
                .build();
        List<ProjectAcceptanceDO> acceptances = projectAcceptanceMapper.list(condition);
        if (CollUtil.isEmpty(acceptances)) {
            return ReturnT.SUCCESS;
        }

        List<Long> projectIds = acceptances.stream().map(ProjectAcceptanceDO::getProjectId).collect(Collectors.toList());
        List<ProjectDO> projects = projectMapper.getByIds(projectIds);
        Set<Long> otnProjectIds = projects.stream()
                .filter(e -> ProjectKindEnum.PBG_OTN.getCode().equals(e.getKind()))
                .map(BaseDO::getId)
                .collect(Collectors.toSet());

        acceptances.stream()
                .filter(e -> otnProjectIds.contains(e.getProjectId()))
                .forEach(e -> {
                    try {
                        ProjectAcceptanceModifyReq req = new ProjectAcceptanceModifyReq();
                        req.setId(e.getId());
                        req.setDesc("验收人员" + autoAcceptanceDay +
                                "天未进行验收处理，系统自动通过验收，若有异议，请联系产研项目经理重新发起验收");
                        projectAcceptanceService.accept(req);
                    } catch (Exception exception) {
                        log.info("[ProjectAutoAcceptanceJob]acceptId:{}, exception:{}", e.getId(), exception.getMessage());
                    }
                });

        return ReturnT.SUCCESS;
    }
}

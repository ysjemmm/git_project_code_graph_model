package com.timevale.forward.service.job;

import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.model.enums.ProjectRiskStatusEnum;
import com.timevale.forward.model.enums.ProjectRiskTypeEnum;
import com.timevale.forward.service.component.ProjectRiskComponent;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/02/07 16:08
 */
@Slf4j
@JobHandler(value = "InnerProjectRiskBackJob")
public class InnerProjectRiskBackJob extends IJobHandler {
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ProjectRiskComponent projectRiskComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> execute(String s) throws Exception {
        log.info("[InnerProjectRiskBackJob] start");

        // 风险
        List<Integer> types = new ArrayList<>();
        types.add(ProjectRiskTypeEnum.MILE_STONE_START.getCode());
        types.add(ProjectRiskTypeEnum.MILE_STONE_END.getCode());
        types.add(ProjectRiskTypeEnum.MILE_STONE_NONE.getCode());

        List<ProjectRiskDO> risks = projectRiskMapper.selectByStatusTypes(ProjectRiskStatusEnum.PENDING.getCode(), types);

        Set<Long> projectIds = risks.stream().map(ProjectRiskDO::getProjectId).collect(Collectors.toSet());
        Set<Long> milestoneIds = risks.stream().map(ProjectRiskDO::getMainId).collect(Collectors.toSet());

        projectIds.forEach(projectRiskComponent::solveNoEntry);
        milestoneIds.forEach(projectRiskComponent::solveRisk);

        log.info("[InnerProjectRiskBackJob] end");
        return ReturnT.SUCCESS;
    }

}

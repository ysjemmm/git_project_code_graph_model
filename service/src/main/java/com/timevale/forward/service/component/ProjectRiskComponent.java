package com.timevale.forward.service.component;

import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.ProjectCategoryEnum;
import com.timevale.forward.model.enums.ProjectRiskStatusEnum;
import com.timevale.forward.model.enums.ProjectRiskTypeEnum;
import com.timevale.forward.model.enums.ProjectStageEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/02/21 15:56
 */
@Slf4j
@Component
public class ProjectRiskComponent {
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ProjectMilestoneComponent milestoneComponent;

    public void solveNoEntry(Long projectId) {
        log.info("[DrcRiskListener.solveNoEntry]处理可能的未录入风险：projectId:{}", projectId);

        // 查询当前项目的里程碑
        List<ProjectMilestoneVO> milestones = milestoneComponent.listByProjectId(projectId);

        // 存在里程碑的阶段
        Set<String> milestoneStageSet = milestones.stream()
                .map(ProjectMilestoneVO::getStage)
                .map(ProjectStageEnum::getTextByCode)
                .collect(Collectors.toSet());

        // 当前项目里程碑未录入风险
        List<ProjectRiskDO> risks = projectRiskMapper.selectByProjectId(projectId);
        Map<String, ProjectRiskDO> noEntryRiskMap = risks.stream()
                .filter(e -> ProjectRiskStatusEnum.PENDING.getCode().equals(e.getStatus())
                        && ProjectRiskTypeEnum.MILE_STONE_NONE.getCode().equals(e.getType()))
                .collect(Collectors.toMap(ProjectRiskDO::getName, e -> e, (a, b) -> a));

        // 内部里程碑的全部阶段
        List<ProjectStageEnum> stageEnumList = Arrays.stream(ProjectStageEnum.values())
                .filter(e -> ProjectCategoryEnum.INNER_PROJECT.equals(e.getCategory()))
                .collect(Collectors.toList());

        for (int i = 0; i < stageEnumList.size(); i++) {
            ProjectStageEnum stageEnum = stageEnumList.get(i);
            ProjectRiskDO riskDO = noEntryRiskMap.get(stageEnum.getText());
            if (riskDO == null) {
                continue;
            }

            // 当前阶段包含里程碑，或者下一阶段不包含里程碑
            Long id = riskDO.getId();
            String name = riskDO.getName();
            ProjectStageEnum nextStageEnum = stageEnumList.get(i + 1);
            if (milestoneStageSet.contains(name) || !milestoneStageSet.contains(nextStageEnum.getText())) {
                projectRiskMapper.updateStatus(id, ProjectRiskStatusEnum.COMPLETE.getCode());
            }
        }
    }
}

package com.timevale.forward.service.job;

import com.timevale.forward.dal.condition.WorkHoursRecordCondition;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.WorkHoursRecordMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.WorkHoursRecordDO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProjectCategoryEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.manager.MessageRetryManager;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@JobHandler("sendWorkHoursSubmitStatisticsJob")
public class SendWorkHoursSubmitStatisticsJob extends IJobHandler {
    private final ProjectMapper projectMapper;
    private final WorkHoursRecordMapper workHoursRecordMapper;
    private final PersonComponent personComponent;
    private final MessageRetryManager messageRetryManager;

    private static final List<Integer> PROJECT_STATUSES = Arrays.asList(
            ProjectStatusEnum.PLANING.getCode(),
            ProjectStatusEnum.DEVING.getCode(),
            ProjectStatusEnum.TESTING.getCode(),
            ProjectStatusEnum.RELEASED.getCode()
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        // 记录任务开始执行的日志
        log.info("[sendWorkHoursSubmitStatisticsJob]开始执行");

        // 1. 查询开启通知的项目列表
        // 根据项目状态和类别查询项目，并筛选出需要通知的项目，按负责人分组
        Map<String, List<ProjectDO>> principalProjectMap = projectMapper.getByWorkHoursNotify(PROJECT_STATUSES, ProjectCategoryEnum.PRODUCT_PROJECT.getCode(), true)
                .stream()
                .filter(e -> StringUtils.isNotEmpty(e.getPrincipalId()))
                .collect(Collectors.groupingBy(ProjectDO::getPrincipalId));

        // 查询所有项目id
        // 将项目列表转换为项目ID列表，以便后续查询
        List<Long> projectIdList = principalProjectMap.values().stream()
                .flatMap(List::stream)
                .map(ProjectDO::getId)
                .collect(Collectors.toList());

        // 如果没有需要处理的项目，则记录日志并返回成功
        if (projectIdList.isEmpty()) {
            log.info("[sendWorkHoursSubmitStatisticsJob]无需处理的项目");
            return ReturnT.SUCCESS;
        }

        // 获取当前日期
        LocalDate today = LocalDate.now();
        // 获取前一天
        LocalDate yesterday = today.minusDays(1);
        // 前一天的开始时间（00:00:00）
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        // 前一天的结束时间（23:59:59.999999999）
        LocalDateTime endOfYesterday = yesterday.atTime(LocalTime.MAX);

        // 查询项目工时记录，并按项目ID分组
        Map<Long, List<WorkHoursRecordDO>> projectWorkHoursMap = workHoursRecordMapper.list(WorkHoursRecordCondition.builder()
                        .projectIds(projectIdList)
                        // 前一天
                        .stratTime(startOfYesterday.format(DATE_TIME_FORMATTER))
                        .endTime(endOfYesterday.format(DATE_TIME_FORMATTER))
                        .build())
                .stream()
                .collect(Collectors.groupingBy(WorkHoursRecordDO::getProjectId));

        // 记录发送消息的计数器
        AtomicInteger sentCount = new AtomicInteger(0);

        // 遍历每个项目负责人及其项目列表，构建并发送消息
        principalProjectMap.forEach((principalId, projectList) -> {
            StringBuilder stringBuilder = new StringBuilder();

            // 遍历每个项目，统计工时填报情况
            for (ProjectDO project : projectList) {
                Long projectId = project.getId();
                List<WorkHoursRecordDO> workHoursRecordDOList = projectWorkHoursMap.getOrDefault(projectId, Collections.emptyList());

                // 初始化统计变量
                int count;
                BigDecimal totalHours;
                Set<String> createManIdSet = new HashSet<>();
                Set<String> createManNameSet = new HashSet<>();

                // 统计每个项目的工时记录
                totalHours = workHoursRecordDOList.stream()
                        .map(WorkHoursRecordDO::getWorkHours)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                for (WorkHoursRecordDO workHoursRecordDO : workHoursRecordDOList) {
                    createManIdSet.add(workHoursRecordDO.getCreateManId());
                    createManNameSet.add(workHoursRecordDO.getCreateMan());
                }

                // 计算填报人次
                count = createManIdSet.size();

                // 查询项目成员列表
                List<PersonDO> personList = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
                List<String> teamMemberList = personList != null ?
                        personList.stream().map(PersonDO::getUserName).collect(Collectors.toList()) :
                        Collections.emptyList();

                // 计算未填报成员列表
                Set<String> filledManSet = new HashSet<>(createManNameSet);
                List<String> unFillManList = teamMemberList.stream()
                        .filter(n -> !filledManSet.contains(n))
                        .collect(Collectors.toList());

                // 构建消息内容
                stringBuilder.append("项目：").append(project.getName()).append("\n")
                        .append("填报人次：").append(count).append("\n")
                        .append("填报工时：").append(totalHours).append("\n")
                        .append("应填报人次：").append(teamMemberList.size()).append("\n")
                        .append("未填报人次：").append(unFillManList.size()).append("\n")
                        .append("未填报项目成员：").append(String.join(",", unFillManList)).append("\n");
            }

            // 构建并发送行动卡片消息
            ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                    .title("工时填报情况")
                    .markdown(stringBuilder.toString())
                    .receivers(Collections.singletonList(principalId))
                    .singleTitle("查看工时填报明细")
                    .singleUrl("dingtalk://dingtalkclient/page/link?url=https://forward.esign.cn&ddtab=true")
                    .build();

            // 异步发送消息
            messageRetryManager.sendAsyncMessage("sendWorkHoursSubmitStatisticsJob", actionCardMsg, principalId, sentCount);
        });

        return ReturnT.SUCCESS;
    }
}

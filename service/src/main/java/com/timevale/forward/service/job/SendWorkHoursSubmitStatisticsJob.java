package com.timevale.forward.service.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.timevale.crm.sdk.common.constant.enums.EnvEnum;
import com.timevale.forward.dal.condition.WorkHoursRecordCondition;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.WorkHoursRecordMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectProductLineBizDomain;
import com.timevale.forward.dal.entity.WorkHoursRecordDO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProjectCategoryEnum;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.integration.ShortLinkClient;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.manager.MessageRetryManager;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.date.WorkDateUtil;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ProductLineMapper productLineMapper;
    private final BizDomainMapper bizDomainMapper;
    private final EnvUtils envUtils;
    private final ShortLinkClient shortLinkClient;
    private final ElapsedTimeClient elapsedTimeClient;
    private final WorkDateUtil workDateUtil;

    private static final List<Integer> PROJECT_STATUSES = Arrays.asList(
            ProjectStatusEnum.PLANING.getCode(),
            ProjectStatusEnum.DEVING.getCode(),
            ProjectStatusEnum.TESTING.getCode(),
            ProjectStatusEnum.RELEASED.getCode()
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 执行发送工作小时提交统计信息的任务
     * 该方法主要用于统计并通知项目成员的工作小时提交情况，只在工作日执行
     * 它会根据项目负责人和业务域来组织信息，并发送给相应的人员
     *
     * @param s 任务参数，未使用
     * @return 返回执行结果，始终为成功
     * @throws Exception 如果执行过程中发生错误
     */
    @Override
    public ReturnT<String> execute(String s) throws Exception {
        // 记录任务开始执行的日志
        log.info("[sendWorkHoursSubmitStatisticsJob]开始执行");

        // 避开节假日
        Date todayDate = new Date();
        List<String> holidays = elapsedTimeClient.getHolidays(todayDate, todayDate, true);
        if (!holidays.isEmpty()) {
            log.warn("[sendWorkHoursSubmitStatisticsJob]今天是节假日，不执行任务");
            return ReturnT.SUCCESS;
        }

        String url;
        if (EnvEnum.PROD.equals(envUtils.getEnv())) {
            url = "https://forward.esign.cn";
        } else if (EnvEnum.PRE.equals(envUtils.getEnv())) {
            url = "https://smlforward.tsign.cn";
        } else {
            url = "https://testforward.tsign.cn";
        }
        // 获取需要通知的工作小时项目
        List<ProjectDO> byWorkHoursNotifyProjects = projectMapper.getByWorkHoursNotify(
                PROJECT_STATUSES, ProjectCategoryEnum.PRODUCT_PROJECT.getCode(), true)
                .stream().filter(e -> ProjectKindEnum.PBG_BASE.getCode().equals(e.getKind()))
                .collect(Collectors.toList());

        // 如果没有需要通知的项目，则结束任务
        if (byWorkHoursNotifyProjects.isEmpty()) {
            log.info("[sendWorkHoursSubmitStatisticsJob]没有需要通知的项目");
            return ReturnT.SUCCESS;
        }

        // 将项目按负责人分组
        Map<String, List<ProjectDO>> principalProjectMap = byWorkHoursNotifyProjects.stream()
                .filter(e -> StringUtils.isNotEmpty(e.getPrincipalId()))
                .collect(Collectors.groupingBy(ProjectDO::getPrincipalId));

        // 获取所有项目ID
        List<Long> projectIdList = principalProjectMap.values().stream()
                .flatMap(List::stream)
                .map(ProjectDO::getId)
                .distinct()
                .collect(Collectors.toList());

        // 如果项目ID列表为空，则结束任务
        if (projectIdList.isEmpty()) {
            log.info("[sendWorkHoursSubmitStatisticsJob]无需处理的项目");
            return ReturnT.SUCCESS;
        }

        // 填充产品线/业务域信息
        Map<String, List<Long>> bizDomainMap = productLineMapper.getByProjectIds(projectIdList)
                .stream().collect(Collectors.groupingBy(ProjectProductLineBizDomain::getBizDomainName, Collectors.mapping(ProjectProductLineBizDomain::getProjectId, Collectors.toList())));

        // 获取业务域名称列表
        List<String> domainNameList = new ArrayList<>(bizDomainMap.keySet());

        // 获取业务域信息
        List<BizDomainDO> bizDomainDOS = Optional.ofNullable(bizDomainMapper.selectByName(domainNameList))
                .orElse(Collections.emptyList());

        // 将业务域按负责人分组
        Map<String, List<String>> domainOwnerIdMap = bizDomainDOS.stream()
                .collect(Collectors.groupingBy(BizDomainDO::getOwnerId,
                        Collectors.mapping(BizDomainDO::getName, Collectors.toList())));

        // 构建业务域负责人与项目ID的映射
        Map<String, Set<Long>> domainOwnerProjectIdMap = new HashMap<>();
        domainOwnerIdMap.forEach((ownerId, domainNames) -> {
            Set<Long> ids = new HashSet<>();
            for (String domainName : domainNames) {
                List<Long> projectIds = bizDomainMap.get(domainName);
                if (projectIds != null) {
                    ids.addAll(projectIds);
                }
            }
            domainOwnerProjectIdMap.put(ownerId, ids);
        });

        // 构建负责人与项目的映射，包括直接负责的项目和通过业务域关联的项目
        Map<String, List<ProjectDO>> ownerProjectMap = new HashMap<>();
        principalProjectMap.forEach((principalId, projects) -> {
            Set<Long> projectIds = domainOwnerProjectIdMap.getOrDefault(principalId, Collections.emptySet());
            List<ProjectDO> mergedList = new ArrayList<>(projects);
            if (!projectIds.isEmpty()) {
                mergedList.addAll(byWorkHoursNotifyProjects.stream()
                        .filter(p -> projectIds.contains(p.getId()))
                        .distinct()
                        .collect(Collectors.toList()));
            }
            ownerProjectMap.put(principalId, mergedList);
        });

        // 对于没有直接负责项目但有业务域关联项目的负责人，构建其项目列表
        domainOwnerProjectIdMap.forEach((ownerId, projectIds) -> {
            if (!principalProjectMap.containsKey(ownerId)) {
                List<ProjectDO> dos = byWorkHoursNotifyProjects.stream()
                        .filter(e -> projectIds.contains(e.getId()))
                        .collect(Collectors.toList());
                ownerProjectMap.put(ownerId, dos);
            }
        });
        // 负责人集合为空，则直接返回成功
        if (CollUtil.isEmpty(ownerProjectMap)) {
            return ReturnT.SUCCESS;
        }

        // 封装昨日时间逻辑
        LocalDate latestWorkday = workDateUtil.getLatestWorkday(todayDate);
        LocalDateTime startOfLatestWorkday = latestWorkday.atStartOfDay();
        LocalDateTime endOfLatestWorkday = latestWorkday.atTime(LocalTime.MAX);
        String startTimeStr = startOfLatestWorkday.format(DATE_TIME_FORMATTER);
        String endTimeStr = endOfLatestWorkday.format(DATE_TIME_FORMATTER);

        // 获取每个项目的工作小时记录
        Map<Long, List<WorkHoursRecordDO>> projectWorkHoursMap = workHoursRecordMapper.list(
                        WorkHoursRecordCondition.builder()
                                .projectIds(projectIdList)
                                .stratTime(startTimeStr)
                                .endTime(endTimeStr)
                                .build())
                .stream()
                .collect(Collectors.groupingBy(WorkHoursRecordDO::getProjectId));

        // 统计发送消息的数量
        AtomicInteger sentCount = new AtomicInteger(0);

        // 遍历每个负责人及其项目，构建并发送消息
        ownerProjectMap.forEach((principalId, projectList) -> {
            StringBuilder stringBuilder = new StringBuilder();

            if (projectList.isEmpty()) {
                return;
            }
            // 找到第一个项目
            Long proId = Optional.of(projectList)
                    .map(list -> list.get(0).getId())
                    .orElse(null);

            // 构建项目工作小时统计信息
            for (ProjectDO project : projectList) {
                Long projectId = project.getId();
                List<WorkHoursRecordDO> records = projectWorkHoursMap.getOrDefault(projectId, Collections.emptyList());

                BigDecimal totalHours = records.stream()
                        .map(WorkHoursRecordDO::getWorkHours)
                        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b, MathContext.DECIMAL64));

                Set<String> filledUserIds = records.stream()
                        .map(WorkHoursRecordDO::getCreateManId)
                        .collect(Collectors.toSet());

                Set<String> filledNames = records.stream()
                        .map(WorkHoursRecordDO::getCreateMan)
                        .collect(Collectors.toSet());

                int filledCount = filledUserIds.size();

                List<PersonDO> personList = Optional.ofNullable(personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode()))
                        .orElse(Collections.emptyList());

                List<String> teamMembers = personList.stream()
                        .map(PersonDO::getUserName)
                        .collect(Collectors.toList());

                List<String> unfilled = teamMembers.stream()
                        .filter(n -> !filledNames.contains(n))
                        .collect(Collectors.toList());

                stringBuilder.append("项目：").append(project.getName()).append("  \n")
                        .append("填报人次：").append(filledCount).append("  \n")
                        .append("填报工时：").append(totalHours.setScale(2, RoundingMode.HALF_UP)).append("  \n")
                        .append("应填报人次：").append(teamMembers.size()).append("  \n")
                        .append("未填报人次：").append(unfilled.size()).append("  \n")
                        .append("未填报项目成员：").append(String.join(",", unfilled)).append("  \n");
            }

            // 如果负责人没有需要发送的消息内容，则记录日志并跳过
            if (stringBuilder.length() == 0) {
                log.warn("[sendWorkHoursSubmitStatisticsJob]负责人 {} 没有需要发送的消息内容", principalId);
                return;
            }

            // 构建并发送消息
            String urlStr = url + "/projectManagement/edit?id=" + proId + "&type=check&tabActive=7";
            String dateStr = latestWorkday.format(DATE_FORMATTER);
            // 生成短链接
            String shortUrl = shortLinkClient.getShortUrl(urlStr).getShortlink();
            // 构建钉钉外部浏览器跳转链接
            String dingtalkUrl = "dingtalk://dingtalkclient/page/link?url=" + shortUrl + "&pc_slide=false";

            // 构建并发送消息
            ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                    .title("工时填报情况")
                    .markdown(buildMarkdownMessage(stringBuilder, dateStr, dingtalkUrl))
                    .receivers(Collections.singletonList(principalId))
                    .singleTitle("查看工时填报明细")
                    .singleUrl(dingtalkUrl)
                    .build();

            messageRetryManager.sendAsyncMessage("sendWorkHoursSubmitStatisticsJob", actionCardMsg, principalId, sentCount);
        });

        // 任务执行成功
        return ReturnT.SUCCESS;
    }

    private String buildMarkdownMessage(StringBuilder content, String dateStr, String fullUrl) {
        return "## " + dateStr + "工时填报情况  \n" +
                content +
                "  \n👉 [点击跳转填报详情页面](" + fullUrl + ")  \n" +
                "⚠️ 如跳转失败，请复制下方链接在浏览器打开：  \n" +
                fullUrl;
    }
}

package com.timevale.forward.service.job;

import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProjectCategoryEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.copy.TaskCopier;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.manager.MessageRetryManager;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@JobHandler("sendWorkHoursSubmitJob")
public class SendWorkHoursSubmitJob extends IJobHandler {
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final PersonMapper personMapper;
    private final MessageRetryManager messageRetryManager;

    private static final List<Integer> PROJECT_STATUSES = Arrays.asList(
            ProjectStatusEnum.PLANING.getCode(),
            ProjectStatusEnum.DEVING.getCode(),
            ProjectStatusEnum.TESTING.getCode(),
            ProjectStatusEnum.RELEASED.getCode()
    );

    private static final List<Integer> TASK_STATUSES = Arrays.asList(
            TaskStatusEnum.WAITING.getCode(),
            TaskStatusEnum.PROGRESS.getCode()
    );

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        // 开始执行定时任务的日志记录
        log.info("[sendWorkHoursSubmitJob]开始执行");

        // 避开休息日
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zoneId);
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            log.warn("[sendWorkHoursSubmitJob]今天是周六或周日，不执行任务");
            return ReturnT.SUCCESS;
        }

        // 1. 查询开启通知的项目列表
        // 通过项目状态和类别查询项目，并过滤出需要工时通知的项目，将其ID与名称映射为Map
        Map<Long, String> notifyProjectMap = projectMapper.getByWorkHoursNotify(PROJECT_STATUSES, ProjectCategoryEnum.PRODUCT_PROJECT.getCode(), true)
                .stream()
                .collect(Collectors.toMap(ProjectDO::getId, ProjectDO::getName, (e1, e2) -> e1));

        // 如果没有找到开启工时通知的项目，则记录日志并结束执行
        if (notifyProjectMap.isEmpty()) {
            log.warn("[sendWorkHoursSubmitJob]未找到开启工时通知的项目");
            return ReturnT.SUCCESS;
        }

        // 将通知项目的ID收集到列表中
        List<Long> projectIds = new ArrayList<>(notifyProjectMap.keySet());

        // 2. 查询项目中待通知的任务
        // 根据项目ID列表和其他条件查询待处理的任务列表
        List<TaskDO> progressTaskList = taskMapper.getProgressTaskList(TaskListCondition.builder()
                .projectIds(projectIds)
                .status(TASK_STATUSES)
                .currentDate(new Date())
                .build());

        // 如果没有找到待通知的任务，则记录日志并结束执行
        if (progressTaskList.isEmpty()) {
            log.warn("[sendWorkHoursSubmitJob]未找到待通知的任务");
            return ReturnT.SUCCESS;
        }

        // 将待通知任务的ID收集到列表中
        List<Long> progressTaskIdList = progressTaskList.stream()
                .map(TaskDO::getId)
                .collect(Collectors.toList());

        // 3. 获取执行人信息
        // 根据任务ID列表查询任务执行人信息，并按执行人ID分组
        Map<String, List<PersonDO>> executorMap = personMapper.get(progressTaskIdList, PersonTypeEnum.TASK_EXECUTOR.getCode())
                .stream()
                .collect(Collectors.groupingBy(PersonDO::getUserId));

        // 将任务信息转换为任务视图对象并映射为Map
        Map<Long, TaskVO> taskVOMap = TaskCopier.INSTANCE.convert(progressTaskList).stream()
                .collect(Collectors.toMap(TaskVO::getId, Function.identity(), (e1, e2) -> e1));

        // 4. 构建并发送消息
        // 遍历每个执行人，构建并发送行动卡片消息
        AtomicInteger sentCount = new AtomicInteger(0);
        executorMap.forEach((userId, executors) -> {
            List<Long> taskIdList = executors.stream()
                    .map(PersonDO::getMainId)
                    .filter(taskVOMap::containsKey)
                    .collect(Collectors.toList());

            // 如果执行人没有待处理任务，则跳过
            if (taskIdList.isEmpty()) {
                return;
            }

            // 构建消息内容，列出待完成任务项
            StringBuilder stringBuilder = new StringBuilder("待完成任务项:  \n");
            taskIdList.forEach(taskId -> {
                TaskVO vo = taskVOMap.get(taskId);
                stringBuilder.append("【").append(notifyProjectMap.get(vo.getProjectId())).append("】").append("-").append(vo.getName()).append("  \n");
            });

            // 创建行动卡片消息对象
            ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                    .title("工时填报")
                    .markdown(stringBuilder.toString())
                    .receivers(Lists.newArrayList(userId))
                    .singleTitle("去填报")
                    .singleUrl("dingtalk://dingtalkclient/page/link?url=https://forward.esign.cn&ddtab=true")
                    .build();

            messageRetryManager.sendAsyncMessage("sendWorkHoursSubmitJob", actionCardMsg, userId, sentCount);
        });

        return ReturnT.SUCCESS;
    }
}

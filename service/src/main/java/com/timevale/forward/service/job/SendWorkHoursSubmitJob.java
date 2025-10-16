package com.timevale.forward.service.job;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.WorkDateUtil;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@JobHandler("sendWorkHoursSubmitJob")
public class SendWorkHoursSubmitJob extends IJobHandler {
    private final ElapsedTimeClient elapsedTimeClient;
    private final WorkDateUtil workDateUtil;
    private final ProjectService projectService;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        // 开始执行定时任务的日志记录
        log.info("[sendWorkHoursSubmitJob]开始执行");
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        // 避开节假日
        Date todayDate = new Date();
        List<String> holidays = elapsedTimeClient.getHolidays(todayDate, todayDate, true);
        if (!holidays.isEmpty()) {
            log.warn("[sendWorkHoursSubmitJob]今天是节假日，不执行任务");
            return ReturnT.SUCCESS;
        }

        LocalDate today;
        boolean isExpedite = StringUtils.isNotEmpty(s) && JSON.parseObject(s).getBoolean("isExpedite");
        if (isExpedite) {
            today = workDateUtil.getLatestWorkday(todayDate);
        } else {
            today = LocalDate.now(zoneId);
        }

        projectService.sendWorkHourNotice(today, isExpedite, null);

        return ReturnT.SUCCESS;
    }
}

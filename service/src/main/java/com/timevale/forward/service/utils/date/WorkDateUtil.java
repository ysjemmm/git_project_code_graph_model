package com.timevale.forward.service.utils.date;

import cn.hutool.core.date.DateUtil;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class WorkDateUtil {

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 在集合workdays中获取最新一天的日期
     */
    public LocalDate getLatestWorkday(Date todayDate) {
        // 得到最近30天之前的日期
        Date offsetDay = cn.hutool.core.date.DateUtil.offsetDay(todayDate, -30);
        Date yesterday = DateUtil.offsetDay(todayDate, -1);
        List<String> workdays = elapsedTimeClient.getHolidays(offsetDay, yesterday, false);
        // 默认返回昨天
        LocalDate defaultDate = LocalDate.from(yesterday.toInstant().atZone(ZoneId.systemDefault()));
        if (workdays == null || workdays.isEmpty()) {
            // yesterday转为LocalDate
            return defaultDate;
        }

        return workdays.stream()
                .map(dateStr -> {
                    try {
                        return LocalDate.parse(dateStr, DATE_FORMATTER);
                    } catch (Exception e) {
                        log.warn("日期解析失败: {}", dateStr, e);
                        return defaultDate;
                    }
                })
                .max(Comparator.naturalOrder())
                .orElse(defaultDate);
    }
}

package com.timevale.forward.service.integration;

import com.timevale.mandarin.base.util.DateUtils;
import com.timevale.soar.facade.api.HolidayQueryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoarClient {

    private final HolidayQueryClient holidayClient;

    private static final Map<String, Boolean> holidayCache = new ConcurrentHashMap<>(32);

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    public List<String> getHolidays(Date startTime, Date endTime) {
        Date dayEnd = DateUtils.getDayEnd(endTime);
        Date dayStart = DateUtils.getDayBegin(startTime);
        if (dayStart.after(dayEnd)) {
            return Collections.emptyList();
        }
        List<String> holidays = new ArrayList<>();
        while (dayStart.before(dayEnd)) {
            String dayStr = DATE_FORMAT.format(dayStart);
            if (isHoliday(dayStr)) {
                holidays.add(dayStr);
            }
            dayStart = DateUtils.addDays(dayStart, 1);
        }
        return holidays;
    }

    /**
     * 判断是否为节假日，日期格式：yyyy-MM-dd
     */
    public boolean isHoliday(String date) {
        if (StringUtils.isBlank(date) || date.length() > 10) {
            return false;
        }
        return Objects.equals(holidayCache.computeIfAbsent(date, k ->
                holidayClient.isHoliday(k).getData()), true);
    }

}

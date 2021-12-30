package com.timevale.forward.service.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/30 17:48
 */
public class DateUtil {

    /**
     * 获取当天最小时间
     *
     * @param date 日期
     * @return Date
     */
    public static Date getStartOfDay(Date date){
        if(date == null){return null;}
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取当天最大时间
     *
     * @param date 日期
     * @return Date
     */
    public static Date getEndOfDay(Date date){
        if(date == null){return null;}
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }
}

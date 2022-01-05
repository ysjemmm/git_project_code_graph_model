package com.timevale.forward.service.utils.date;

import java.util.Calendar;
import java.util.Locale;

/**
 * @author yuankai
 * @date 2020/11/25 10:20
 */
public interface DateFormatConst {
    /**
     * 默认日期格式(24小时制):yyyy-MM-dd HH:mm:ss
     */
    String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 12小时制日期格式:yyyy-MM-dd hh:mm:ss
     */
    String DATE_FORMAT_T12 = "yyyy-MM-dd hh:mm:ss";
    /**
     * 年月日格式:yyyy-MM-dd
     */
    String DATE_FORMAT = "yyyy-MM-dd";
    /**
     * 24小时制时间格式:HH:mm:ss
     */
    String TIME_FORMAT = "HH:mm:ss";
    /**
     * 12小时制时间格式:hh:mm:ss
     */
    String TIME_FORMAT_T12 = "hh:mm:ss";
    /**
     * 默认时区中国大陆:Locale.SIMPLIFIED_CHINESE
     */
    Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    /**
     * 精确到小时的时间格式:yyyy-MM-dd HH
     */
    String DATE_FORMAT_YEAR_TO_HOUR = "yyyy-MM-dd HH:mm";

    /**
     * 精确到分钟的时间格式:yyyy-MM-dd HH:mm
     */
    String DATE_FORMAT_YEAR_TO_MIN = "yyyy-MM-dd HH:mm";

    /**
     * 设置一周的第一天为周一
     */
    int FIRST_DAY_OF_WEEK = Calendar.MONDAY;

    /**
     * 一毫秒
     */
    long ONE_MILLIS = 1L;

    /**
     * 一秒的毫秒数
     */
    long ONE_SECOND = 1000L;
    /**
     * 一分钟的毫秒数
     */
    long ONE_MINUTE = 60 * ONE_SECOND;
    /**
     * 一小时的毫秒数
     */
    long ONE_HOUR = 60 * ONE_MINUTE;
    /**
     * 一天的毫秒数
     */
    long ONE_DAY = 24 * ONE_HOUR;
    /**
     * 一星期的毫秒数
     */
    long ONE_WEEK = 7 * ONE_DAY;

    /**
     * 一个月(30天)的毫秒数
     */
    long ONE_MONTH = 30 * ONE_DAY;

    /**
     * 一年(365天)的毫秒数
     */
    long ONE_YEAR = 365 * ONE_DAY;
}

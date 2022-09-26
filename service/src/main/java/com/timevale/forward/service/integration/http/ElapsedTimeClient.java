package com.timevale.forward.service.integration.http;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
public interface ElapsedTimeClient {
    /**
     * 得到时间（工作时长计算）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 时间戳
     */
    Long getElapsedTime(Date startTime, Date endTime);

    /**
     * 计算结束时间(工作时长计算)
     *
     * @param startTime 开始时间
     * @param seconds   结束时间
     * @return 日期
     */
    String getElapsedEndTime(Date startTime, Long seconds);

    /**
     *
     * @param startTime  开始时间
     * @param endTime 结束时间
     * @param holiday true 节假日 false 工作日
     * @return 日期
     */
    List<String> getHolidays(Date startTime, Date endTime,boolean holiday);

}

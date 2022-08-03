package com.timevale.forward.service.integration.http;

import java.util.Date;

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
     * @param seconds   s
     * @return 时间戳
     */
    String getElapsedEndTime(Date startTime, Long seconds);

}

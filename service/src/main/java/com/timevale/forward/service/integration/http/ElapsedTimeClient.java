package com.timevale.forward.service.integration.http;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
public interface ElapsedTimeClient {
    /**
     * 得到时间
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 时间戳
     */
    Long getElapsedTime(Date startTime, Date endTime);

}

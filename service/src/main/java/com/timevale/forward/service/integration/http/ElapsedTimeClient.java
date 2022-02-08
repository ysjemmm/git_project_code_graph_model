package com.timevale.forward.service.integration.http;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
public interface ElapsedTimeClient {


    Long getElapsedTime(Date startTime, Date endTime);

}

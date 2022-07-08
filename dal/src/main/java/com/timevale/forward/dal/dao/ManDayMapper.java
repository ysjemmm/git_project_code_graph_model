package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ManDayDO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author jingchun
 * created on 2022-06-20 14:51:04
 */
public interface ManDayMapper {

    List<ManDayDO> getByProjectId(@Param("projectId") Long projectId);

    List<ManDayDO> getByProjectIdAndStartDates(@Param("projectId") Long projectId,
                                               @Param("startDates") Collection<Date> startDate,
                                               @Param("memberIds") Collection<String> memberIds);

    List<ManDayDO> getByStartDate(@Param("startDate") Date startDate);

    void delete(ManDayDO manDayDO);

    void updateActualManDay(ManDayDO manDayDO);

    void insert(ManDayDO setWeekEndDate);

    BigDecimal sumProjectActualDays(@Param("projectId") Long projectId);
}

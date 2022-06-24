package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ManDayDO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
* @author jingchun
* created on 2022-06-20 14:51:04
*/
public interface ManDayMapper {

    List<ManDayDO> getByProjectId(@Param("projectId") Long projectId);

    List<ManDayDO> getByProjectIdAndDateRange(@Param("projectId") Long projectId,
                                              @Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);

    List<ManDayDO> getByMemberIdAndDateRange(@Param("memberId") String memberId,
                                             @Param("startDate") Date startDate,
                                             @Param("endDate") Date endDate);
}





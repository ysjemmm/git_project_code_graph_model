package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ManDayReportCondition;
import com.timevale.forward.dal.entity.ManDayReportDO;
import com.timevale.forward.dal.entity.ManDayReportListDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/19 16:37
 */
public interface ManDayReportMapper {

    void insert(ManDayReportDO manDayReportDO);

    ManDayReportDO selectById(@Param("id") Long id);

    ManDayReportDO selectByManDayId(@Param("manDayId") Long manDayId);

    ManDayReportDO selectByStatus(@Param("manDayId") Long manDayId, @Param("auditStatus") Integer auditStatus);

    List<ManDayReportDO> selectByManDayIds(@Param("manDayIds") List<Long> manDayIds);

    List<ManDayReportDO> selectByIds(@Param("ids") List<Long> ids);

    List<ManDayReportListDO> selectCondition(ManDayReportCondition condition);

    void updateById(ManDayReportDO manDayReportDO);

    void updateStatus(@Param("idList") List<Long> ids, @Param("auditStatus") Integer auditStatus);

}

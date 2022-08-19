package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ManDayReportCondition;
import com.timevale.forward.dal.entity.ManDayReportDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/19 16:37
 */
public interface ManDayReportMapper {

    void insert(ManDayReportDO manDayReportDO);

    ManDayReportDO selectById(@Param("id") Long id);

    List<ManDayReportDO> selectByIds(@Param("ids") List<Long> ids);

    List<ManDayReportDO> selectCondition(ManDayReportCondition condition);

    void updateById(ManDayReportDO manDayReportDO);

    void updateStatus(@Param("idList") List<Long> ids, @Param("auditStatus") Integer auditStatus);

}

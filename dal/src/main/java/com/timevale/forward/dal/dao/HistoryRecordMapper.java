package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.HistoryRecordDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author by YangXu
 * @date 2023/03/10 14:30
 */
public interface HistoryRecordMapper {

    void insert(HistoryRecordDO recordDO);

    HistoryRecordDO selectLast(@Param("projectId")Long projectId);

    List<HistoryRecordDO> selectByProjectId(@Param("projectId")Long projectId);

    @Select("SELECT * FROM history_record WHERE project_id=#{projectId} AND version =#{version} AND is_deleted=false")
    HistoryRecordDO selectByVersion(@Param("projectId")Long projectId, @Param("version") BigDecimal version);

}

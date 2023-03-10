package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.HistoryRecordDO;
import org.apache.ibatis.annotations.Param;

/**
 * @author by YangXu
 * @date 2023/03/10 14:30
 */
public interface HistoryRecordMapper {

    void insert(HistoryRecordDO recordDO);

    HistoryRecordDO selectLast(@Param("projectId")Long projectId);
}

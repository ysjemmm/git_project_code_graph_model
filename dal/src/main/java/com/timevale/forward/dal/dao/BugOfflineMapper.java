package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BugOfflineDO;
import org.apache.ibatis.annotations.Param;

/**
 * @author by YangXu
 * @date 2022/02/24 09:35
 */
public interface BugOfflineMapper {
    /**
     * 插入
     *
     * @param bugOfflineDO 线下bugDO
     * @return 影响行数
     */
    int insert(@Param("bugOfflineDO") BugOfflineDO bugOfflineDO);
}

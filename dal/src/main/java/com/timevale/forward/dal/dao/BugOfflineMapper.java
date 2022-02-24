package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.entity.BugOfflineDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 根据查询条件获取DO列表
     *
     * @param bugOfflineListCondition 线下bug查询列表条件
     * @return BugOfflineDO List
     */
    List<BugOfflineDO> selectByCondition(@Param("condition") BugOfflineListCondition bugOfflineListCondition);

    /**
     * 更具线下bug的id获取到线下bug的详情信息
     *
     * @param bugOfflineId 线下bug的id
     * @return BugOfflineDO 返回数据
     */
    BugOfflineDO selectById(@Param("bugOfflineId") Long bugOfflineId);
}

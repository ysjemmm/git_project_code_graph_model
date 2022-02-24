package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOfflineListDO;
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
    int insert(BugOfflineDO bugOfflineDO);

    /**
     * 更新
     *
     * @param bugOfflineDO 线下bugDO
     * @return 影响行数
     */
    int update(BugOfflineDO bugOfflineDO);

    /**
     * 选择id获取DO
     *
     * @param userIds 用户id
     * @return BugOfflineDO list
     */
    List<BugOfflineDO> selectByMembers(@Param("userIds") List<String> userIds);

    /**
     * 选择id获取DO
     *
     * @param id id
     * @return BugOfflineDO
     */
    BugOfflineDO selectById(@Param("id") Long id);

    /**
     * 根据查询条件获取DO列表
     *
     * @param bugOfflineListCondition 线下bug查询列表条件
     * @return BugOfflineListDO List
     */
    List<BugOfflineListDO> selectByCondition(BugOfflineListCondition bugOfflineListCondition);

    /**
     * 根据bug的id删除线下bug
     *
     * @param bugOfflineId 参数
     * @return Boolean 返回值
     */
    Boolean deleteById(@Param("bugOfflineId") Long bugOfflineId);

}

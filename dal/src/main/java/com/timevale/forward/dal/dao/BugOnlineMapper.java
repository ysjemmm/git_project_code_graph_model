package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BugOnlineListCondition;
import com.timevale.forward.dal.entity.BugOnlineListDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Date 2022/3/18 11:14
 * @Author 望轩
 */
public interface BugOnlineMapper {
    /**
     * 根据线上bug的id查询线上bug
     *
     * @param id 线上bug的id
     * @return 返回值
     */
    BugOnlineDO selectById(@Param("id") Long id);

    /**
     * 选择列表通过条件
     *
     * @param bugOnlineListCondition 线上bug列表条件
     * @return BugOnlineListDO 列表
     */
    List<BugOnlineListDO> selectListByCondition(BugOnlineListCondition bugOnlineListCondition);

    /**
     * 更新线上bug
     *
     * @param bugOnlineDO 参数
     */
    void update(@Param("bugOnlineDO") BugOnlineDO bugOnlineDO);

    /**
     * 完整更新
     *
     * @param bugOnlineDO 线上bugDO
     */
    void fullUpdate(@Param("bugOnlineDO") BugOnlineDO bugOnlineDO);

    /**
     * 插入线上bug
     *
     * @param bugOnlineDO 参数
     * @return Long
     */
    Long insert(@Param("bugOnlineDO") BugOnlineDO bugOnlineDO);

    /**
     * 根据线上bug的业务需求id查询线上bug
     *
     * @param bizDemandId 业务需求id
     * @return 返回值
     */
    BugOnlineDO selectByBizDemandId(@Param("bizDemandId") Long bizDemandId);

    /**
     * 根据线上bug的id查询线上bug
     *
     * @param linkBugId 关联的线上bug的id
     * @return 返回值
     */
    List<BugOnlineDO> selectByLinkBugId(@Param("linkBugId") Long linkBugId);

    /**
     * 批量更新关联的bug
     * @param ids ids
     * @param linkBugId linkBugId
     * @return int
     */
    int updateByIds(@Param("ids") List<Long> ids,@Param("linkBugId") Long linkBugId);

    /**
     * 根据线上bug的id查询线上bug
     *
     * @param ids 线上bug的id
     * @return 返回值
     */
    List<BugOnlineDO> selectByIds(@Param("ids") List<Long> ids);
}

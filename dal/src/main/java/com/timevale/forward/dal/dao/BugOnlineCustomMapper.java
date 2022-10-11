package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BugOnlineCustomDO;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description:
 * @ClassName: BugOnlineCustomMapper
 * @Author yexuan
 * @Date  2022-09-23 15:18
 */
public interface BugOnlineCustomMapper {

    /**
     * 批量插入数据
     *
     * @param bugOnlineCustomDOList 参数
     */
    void batchInsert(@Param("bugOnlineCustomDOList") List<BugOnlineCustomDO> bugOnlineCustomDOList);

    /**
     * 逻辑删除
     * @param bugOnlineCustomDO
     */
    void delete(@Param("bugOnlineCustomDO") BugOnlineCustomDO bugOnlineCustomDO);

    /**
     * 根据线上Bug id查询
     * @param bugOnlineId
     * @return
     */
    List<BugOnlineCustomDO> selectByBugOnlineId(@Param("bugOnlineId") Long bugOnlineId);
}

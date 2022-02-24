package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BugLogDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Date 2022/2/24 14:13
 * @Author 望轩
 */
public interface BugLogMapper {
    /**
     * 通过线下bug的id查询对应的操作日志
     *
     * @param bugOfflineId 线下bug的id
     * @return List<BugLogDO> 返回类型
     */
    List<BugLogDO> selectByBugOfflineId(@Param("bugOfflineId") Long bugOfflineId);

    /**
     * 通过线下bug的id删除对应的操作日志
     *
     * @param bugOfflineId 线下bug的id
     * @return Boolean 返回值
     */
    Boolean deleteByBugOfflineId(@Param("bugOfflineId") Long bugOfflineId);
}
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
     * 通过线下bug的id和type类型查询对应的操作日志
     *
     * @param bugOfflineId 线下bug的id
     * @param type         bug类型
     * @param statusChange 是否为bug状态变更
     * @return List<BugLogDO> 返回类型
     */
    List<BugLogDO> selectByBugOfflineIdAndType(@Param("bugOfflineId") Long bugOfflineId, @Param("type") Integer type, @Param("statusChange") Boolean statusChange);

    /**
     * 查询bug日志
     *
     * @param mainIdList 主要id列表
     * @param type       类型
     */
    List<BugLogDO> selectBugStatusLog(@Param("mainIdList") List<Long> mainIdList, @Param("type") Integer type);

    /**
     * 通过线下bug的id删除对应的操作日志
     *
     * @param bugId bug的id
     * @param type  变更类型
     * @return Boolean 返回值
     */
    Boolean deleteByBugId(@Param("bugId") Long bugId, @Param("type") Integer type);

    /**
     * 往bug日志表中插入数据
     *
     * @param bugLogDO 参数
     * @return 返回影响行数
     */
    Integer insert(@Param("bugLogDO") BugLogDO bugLogDO);

    /**
     * 批量 往bug日志表中插入数据
     *
     * @param bugLogDOList 参数
     * @return 返回影响行数
     */
    int batchInsert(@Param("bugLogDOList") List<BugLogDO> bugLogDOList);
}
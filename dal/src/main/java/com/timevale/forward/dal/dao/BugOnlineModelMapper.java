package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BugOnlineModelDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Date 2022/3/18 11:50
 * @Author 望轩
 */
public interface BugOnlineModelMapper {

    /**
     * 通过线上bug的id查询出模块id集合
     *
     * @param id 线上bug的id
     * @return 返回值
     */
    List<Long> selectModelIds(@Param("id") Long id);

    /**
     * 选择通过id列表
     *
     * @param bugOnlineIdList 模块
     * @return BugOnlineProductLineDO 列表
     */
    List<BugOnlineModelDO> selectByBugOnlineIdList(@Param("bugOnlineIdList") List<Long> bugOnlineIdList);

    /**
     * 批量插入数据
     *
     * @param bugOnlineModelDOList 参数
     */
    void batchInsert(@Param("list") List<BugOnlineModelDO> bugOnlineModelDOList);

    /**
     * 更新数据
     *
     * @param bugOnlineModelDO 参数
     */
    void update(BugOnlineModelDO bugOnlineModelDO);
}

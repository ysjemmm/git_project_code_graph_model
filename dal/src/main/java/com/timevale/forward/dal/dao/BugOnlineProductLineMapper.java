package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BugOnlineProductLineDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Date 2022/3/18 11:50
 * @Author 望轩
 */
public interface BugOnlineProductLineMapper {

    /**
     * 通过线上bug的id查询出产品线id集合
     *
     * @param id 线上bug的id
     * @return 返回值
     */
    List<Long> selectProductLineIds(@Param("id") Long id);

    /**
     * 选择通过id列表
     *
     * @param bugOnlineIdList 错误在线id列表
     * @return BugOnlineProductLineDO 列表
     */
    List<BugOnlineProductLineDO> selectByBugOnlineIdList(@Param("bugOnlineIdList") List<Long> bugOnlineIdList);

    /**
     * 批量插入数据
     *
     * @param bugOnlineProductLineDOList 参数
     */
    void batchInsert(@Param("bugOnlineProductLineDOList") List<BugOnlineProductLineDO> bugOnlineProductLineDOList);

    /**
     * 更新数据
     *
     * @param bugOnlineProductLineDO 参数
     */
    void update(@Param("bugOnlineProductLineDO") BugOnlineProductLineDO bugOnlineProductLineDO);
}

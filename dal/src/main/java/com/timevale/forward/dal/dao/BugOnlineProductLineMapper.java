package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BugOnlineLinkCondition;
import com.timevale.forward.dal.entity.BugOnlineProductLineDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    List<Long> selectProductLineIds(@Param("id") Long id,@Param("type") Integer type);

    /**
     * 选择通过id列表
     *
     * @param bugOnlineIdList 错误在线id列表
     * @return BugOnlineProductLineDO 列表
     */
    List<BugOnlineProductLineDO> getByBugOnlineIdList(@Param("bugOnlineIdList") List<Long> bugOnlineIdList, @Param("type") Integer type);

    @Select("SELECT * FROM bug_online_product_line WHERE bug_online_id = #{bugId} AND type=#{type} AND is_deleted = false")
    List<BugOnlineProductLineDO> getByBugOnlineId(@Param("bugId") Long bugId, @Param("type") Integer type);

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

    /**
     * 选择 by 条件
     *
     * @param condition 条件
     * @return {@link List}<{@link Long}>
     */
    List<Long> selectByCondition(BugOnlineLinkCondition condition);
}

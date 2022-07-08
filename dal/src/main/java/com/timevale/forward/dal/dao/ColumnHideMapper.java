package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ColumnHideDO;
import org.apache.ibatis.annotations.Param;

/**
 * @author by YangXu
 * @date 2022/06/24 10:16
 */
public interface ColumnHideMapper {
    /**
     * 插入
     *
     * @param columnHideDO 列隐藏DO
     */
    void insert(ColumnHideDO columnHideDO);

    /**
     * 更新
     *
     * @param columnHideDO 列隐藏DO
     */
    void update(ColumnHideDO columnHideDO);

    /**
     * 根据id更新
     *
     * @param columnHideDO 列隐藏DO
     */
    void updateById(ColumnHideDO columnHideDO);

    /**
     * 查询隐藏列
     *
     * @param model       模型
     * @param tabType     标签类型
     * @param belongManId 属于用户id
     * @return {@link ColumnHideDO}
     */
    ColumnHideDO select(@Param("model") Integer model, @Param("tabType") Integer tabType, @Param("belongManId") String belongManId);
}

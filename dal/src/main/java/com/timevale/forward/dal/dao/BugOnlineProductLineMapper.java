package com.timevale.forward.dal.dao;

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
}

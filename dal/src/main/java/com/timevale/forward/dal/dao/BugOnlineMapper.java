package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BugOnlineDO;
import org.apache.ibatis.annotations.Param;

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
}

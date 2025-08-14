package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ViewsDO;
import org.apache.ibatis.annotations.Param;

/**
 * 视图Mapper
 *
 * @author by qiyuan
 * @date 2025/08/14 14:42
 */
public interface ViewsMapper {

    /**
     * 根据主键id获取视图
     *
     * @param id 主键id
     * @return 视图DO
     */
    ViewsDO get(@Param("id") Long id);


    /**
     * 根据类型和名称查询视图
     *
     * @param type 类型
     * @param name 分组名称
     * @return 视图DO
     */
    ViewsDO getByTypeAndName(@Param("type") Integer type, @Param("name") String name);

    /**
     * 新增一条视图
     *
     * @param viewsDO 视图DO
     * @return 影响行数
     */
    int insert(ViewsDO viewsDO);

    /**
     * 更新视图
     *
     * @param viewsDO 视图DO
     * @return 影响行数
     */
    int update(ViewsDO viewsDO);

    /**
     * 删除视图
     *
     * @param id          主键id
     * @param modifyManId 修改人id
     * @param modifyMan   修改人
     * @return 影响行数
     */
    int delete(@Param("id") Long id, @Param("modifyManId") String modifyManId, @Param("modifyMan") String modifyMan);
}
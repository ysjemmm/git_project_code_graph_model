package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ViewsUserDO;
import org.apache.ibatis.annotations.Param;

/**
 * 视图用户Mapper
 *
 * @author by qiyuan
 * @date 2025/08/14 14:42
 */
public interface ViewsUserMapper {

    /**
     * 根据主键id获取视图用户
     *
     * @param id 主键id
     * @return 视图用户DO
     */
    ViewsUserDO get(@Param("id") Long id);

    /**
     * 根据视图id和用户id获取视图用户
     *
     * @param viewsId 视图id
     * @param ownerId 用户id
     * @return 视图用户DO
     */
    ViewsUserDO getByViewIdAndOwnerId(@Param("viewsId") Long viewsId, @Param("ownerId") String ownerId);

    /**
     * 新增视图用户
     *
     * @param viewsUserDO 视图用户DO
     * @return 影响行数
     */
    int insert(ViewsUserDO viewsUserDO);

    /**
     * 更新视图用户
     *
     * @param viewsUserDO 视图用户DO
     * @return 影响行数
     */
    int update(ViewsUserDO viewsUserDO);

    /**
     * 删除视图用户
     *
     * @param id          主键id
     * @param modifyManId 修改人id
     * @param modifyMan   修改人
     * @return 影响行数
     */
    int delete(@Param("id") Long id, @Param("modifyManId") String modifyManId, @Param("modifyMan") String modifyMan);

    /**
     * 根据视图id删除视图用户
     *
     * @param viewsId     视图id
     * @param modifyManId 修改人id
     * @param modifyMan   修改人
     * @return 影响行数
     */
    int deleteByViewsId(@Param("viewsId") Long viewsId, @Param("modifyManId") String modifyManId, @Param("modifyMan") String modifyMan);


} 
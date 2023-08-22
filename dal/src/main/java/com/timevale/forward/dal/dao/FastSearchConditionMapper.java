package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.FastSearchConditionDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @Description: 快捷搜索条件Mapper
 * @ClassName: FastSearchConditionMapper
 * @Author: shaoye
 * @Date: 2023-08-22 14:41
 */
public interface FastSearchConditionMapper {

    /**
     * 某用户快捷搜索记录是否存在
     */
    @Select("select count(*) from fast_search_condition where search_user_id=#{searchUserId}")
    boolean isExist(@Param("searchUserId") String searchUserId);
    /**
     * 新增快捷搜索条件
     */
    int insert(FastSearchConditionDO entity);

    /**
     * 获取搜索条件内容
     */
    @Select("select search_condition_content from fast_search_condition where search_user_id=#{searchUserId} limit 1")
    String selectSearchConditionContent(@Param("searchUserId") String searchUserId);

    /**
     * 更新搜索条件内容
     */
    @Update("update fast_search_condition set search_condition_content=#{searchConditionContent} where search_user_id=#{searchUserId}")
    void updateSearchConditionContent(@Param("searchUserId") String searchUserId, @Param("searchConditionContent") String searchConditionContent);

}

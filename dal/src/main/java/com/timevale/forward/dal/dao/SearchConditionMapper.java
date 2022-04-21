package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.SearchConditionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/04/21 17:39
 */
public interface SearchConditionMapper {

    /**
     * 新增搜索条件
     *
     * @param searchConditionDO 搜索条件DO
     */
    int insert(SearchConditionDO searchConditionDO);

    /**
     * 查询 by id
     *
     * @param id id
     */
    SearchConditionDO selectById(@Param("id") Long id);

    /**
     * 更新
     *
     * @param searchConditionDO 搜索条件DO
     */
    int update(SearchConditionDO searchConditionDO);

    /**
     * 查询搜索条件
     *
     * @param model    模型
     * @param tabType  标签类型
     * @param belongManId 属于用户id
     */
    List<SearchConditionDO> select(@Param("model") Integer model, @Param("tabType") Integer tabType, @Param("belongManId") String belongManId);


}

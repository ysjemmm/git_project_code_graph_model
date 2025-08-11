package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.CommentDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/21 18:37
 */
public interface CommentMapper {

    /**
     * 单个查询
     *
     * @param id id
     * @return CommentDO
     */
    CommentDO getById(@Param("id") Long id);

    /**
     * 插入单个评论
     *
     * @param commentDO 评论DO
     * @return int
     */
    int insert(CommentDO commentDO);

    /**
     * 更新
     *
     * @param commentDO commentDO
     * @return int
     */
    int update(CommentDO commentDO);


    /**
     * 查询对应主体评论
     *
     * @param toId 为id
     * @param type 类型
     * @return
     */
    List<CommentDO> select(@Param("toId") Long toId, @Param("type") Integer type);

    /**
     * 删除对应的主体评论
     *
     * @param toId 主体id
     * @param type 类型
     * @return
     */
    Boolean delete(@Param("toId") Long toId, @Param("type") Integer type);

    /**
     * 删除评论
     *
     * @param id 评论id
     * @return
     */
    Boolean deleteById(@Param("id") Long id);
}

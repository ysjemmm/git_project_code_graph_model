package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.TrackMapCondition;
import com.timevale.forward.dal.entity.TrackMapDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface TrackMapMapper {

    /**
     * 获取产品线列表
     *
     * @return 列表
     */
    List<TrackMapDO> selectAllTrackMap();

    /**
     * 新增
     *
     * @param trackMapDO trackMapDO
     * @return int
     */
    int insert(TrackMapDO trackMapDO);


    /**
     * 列表
     *
     * @return 列表
     */
    List<TrackMapDO> select(TrackMapCondition condition);

    /**
     * 删除
     *
     * @return 列表
     */
    int delete(@Param("id")Long id);

    /**
     * 列表
     *
     * @return 列表
     */
    List<TrackMapDO> getChildren(@Param("parentId") List<Long> parentId,@Param("level")Integer level);


    /**
     * 列表
     *
     * @return 列表
     */
    TrackMapDO get(@Param("id")Long id);


}

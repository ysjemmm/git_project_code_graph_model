package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.TrackEventCondition;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.entity.TrackEventDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface TrackEventMapper {


    /**
     * 新增
     *
     * @param trackMapDO trackMapDO
     * @return int
     */
    int insert(TrackEventDO trackMapDO);

    /**
     * 新增
     *
     * @param trackMapDO trackMapDO
     * @return int
     */
    int update(TrackEventDO trackMapDO);

    /**
     * 新增
     *
     * @param trackMapDO trackMapDO
     * @return int
     */
    int updateWithOutModifyMan(TrackEventDO trackMapDO);


    /**
     * 列表
     *
     * @return TrackEventDO
     */
    List<TrackEventDO> select(TrackEventCondition condition);


    /**
     * 列表
     *
     * @return TrackEventDO
     */
    List<TrackEventDO> list(TrackEventListCondition condition);


    /**
     * 列表
     *
     * @param ids id
     * @return TrackEventDO
     */
    List<TrackEventDO> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 列表
     *
     * @return TrackEventDO
     */
    List<TrackEventDO> selectByName(@Param("fullCnNameList") List<String> fullCnNameList, @Param("egNameList") List<String> egNameList);

    /**
     * 列表
     *
     * @param id id
     * @return TrackEventDO
     */
    TrackEventDO get(@Param("id") Long id,@Param("flowId") String flowId);


    /**
     *
     * @param productDemandId productDemandId
     * @return TrackEventDO
     */
    List<TrackEventDO> linkTrackEventList(@Param("productDemandId")Long productDemandId);
}

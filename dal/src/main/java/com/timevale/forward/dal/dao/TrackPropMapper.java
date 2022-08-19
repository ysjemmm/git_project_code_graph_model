package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.TrackPropCondition;
import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.dal.entity.TrackPropDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface TrackPropMapper {


    /**
     * 新增
     *
     * @param trackPropDO trackPropDO
     * @return int
     */
    int insert(TrackPropDO trackPropDO);

    /**
     * 新增事件-属性
     *
     * @param trackPropDOList trackPropDOList
     * @return int
     */
    int batchInsert(List<TrackPropDO> trackPropDOList);

    /**
     * 新增事件-属性-无填充审计信息
     *
     * @param trackPropDOList trackPropDOList
     * @return int
     */
    int batchInsertNotIC(List<TrackPropDO> trackPropDOList);

    /**
     * 列表
     *
     * @return TrackEventDO
     */
    List<TrackPropDO> select(TrackPropCondition condition);

    /**
     * 列表
     *
     * @return TrackEventDO
     */

    /**
     * 列表
     *
     * @return TrackEventDO
     */
    List<TrackPropDO> list(TrackPropListCondition condition);


    /**
     * 列表
     *
     * @param ids id
     * @return TrackEventDO
     */
    List<TrackPropDO> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 新增
     *
     * @param trackPropDO trackPropDO
     * @return int
     */
    int update(TrackPropDO trackPropDO);

    /**
     * 新增
     *
     * @param trackPropDO trackPropDO
     * @return int
     */
    int updateNotIC(TrackPropDO trackPropDO);

    /**
     * 新增
     *
     * @param trackPropDO trackPropDO
     * @return int
     */
    int updateWithOutModifyMan(TrackPropDO trackPropDO);

    /**
     *
     * @param ids ids
     * @param status status
     * @param type type
     * @return
     */
    int batchUpdate(@Param("ids") List<Long> ids,@Param("status")Integer status,@Param("type")Integer type);

}

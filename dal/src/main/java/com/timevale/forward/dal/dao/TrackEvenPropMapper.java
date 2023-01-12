package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.TrackEventPropCondition;
import com.timevale.forward.dal.entity.TrackEventPropDO;
import com.timevale.forward.dal.entity.TrackEventPropItemDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 14:06
 */
public interface TrackEvenPropMapper {

    /**
     * 查询事件-属性
     *
     * @param condition condition
     * @return 列表
     */
    List<TrackEventPropDO> select(TrackEventPropCondition condition);

    /**
     * 选择 by 事件id列表
     *
     * @param eventIdList 事件id列表
     * @return 列表
     */
    List<TrackEventPropDO> selectByEventIdList(@Param("eventIdList")List<Long>eventIdList);

    /**
     * 更新单条事件-属性
     *
     * @param trackEventPropDO trackEventPropDO
     * @return int
     */
    int update(TrackEventPropDO trackEventPropDO);


    /**
     * 新增事件-属性
     *
     * @param trackEventPropDOList trackEventPropDOList
     * @return int
     */
    int batchInsert(List<TrackEventPropDO> trackEventPropDOList);

    /**
     * 新增事件-属性-无填充审计信息
     *
     * @param trackEventPropDOList trackEventPropDOList
     * @return int
     */
    int batchInsertNotIC(List<TrackEventPropDO> trackEventPropDOList);

    /**
     * 查询事件-属性
     *
     * @param eventId eventId
     * @return 列表
     */
    List<TrackEventPropItemDO> getByEventId(@Param("eventId")Long eventId);


}
package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ImprovementMeasureCondition;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/17 10:26
 */
public interface ImprovementMeasureMapper {

    /**
     * 改进措施-插入
     *
     * @param improvementMeasureDO 改进措施DO
     * @return int
     */
    int insert(ImprovementMeasureDO improvementMeasureDO);

    /**
     * 改进措施-更新
     *
     * @param improvementMeasureDO 改进措施DO
     * @return int
     */
    int update(ImprovementMeasureDO improvementMeasureDO);

    /**
     * 改进措施-批量更新状态
     *
     * @param idList 改进措施id列表
     * @return int
     */
    int batchUpdateStatus(@Param("idList") List<Long> idList, @Param("status")Integer status);

    /**
     * 改进措施-查询 by 故障单id
     *
     * @param troubleTicketId 故障单id
     * @return 改进措施 List
     */
    List<ImprovementMeasureDO> selectByTroubleTicketId(@Param("troubleTicketId")Long troubleTicketId);

    /**
     * 改进措施-查询对应条件
     *
     * @param condition 条件
     * @return 改进措施 List
     */
    List<ImprovementMeasureDO> selectByCondition(ImprovementMeasureCondition condition);

    /**
     * 根据ids查询
     * @param ids
     * @return
     */
    List<ImprovementMeasureDO> selectByIds(@Param("ids") List<Long> ids);

}

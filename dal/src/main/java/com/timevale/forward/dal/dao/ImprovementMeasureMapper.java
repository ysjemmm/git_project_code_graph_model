package com.timevale.forward.dal.dao;

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
     * 改进措施-根据id查询
     *
     * @param id 改进措施 id
     * @return 改进措施 List
     */
    ImprovementMeasureDO selectById(@Param("id") Long id);

    /**
     * 改进措施-查询故障单下的改进措施
     *
     * @param troubleTicketId 故障单的id
     * @return 改进措施 List
     */
    List<ImprovementMeasureDO> selectByTroubleTicketId(@Param("troubleTicketId") Long troubleTicketId);

}

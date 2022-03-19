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
     * 改进措施-查询对应条件
     *
     * @param condition 条件
     * @return 改进措施 List
     */
    List<ImprovementMeasureDO> selectByCondition(ImprovementMeasureCondition condition);

}

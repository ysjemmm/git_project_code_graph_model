package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ImprovementMeasureDO;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/19 12:01
 */
public interface ImprovementMeasureComponent {

    /**
     * 更新钉钉待办状态状态
     *
     * @param improvementMeasureDOList 改进措施DO List
     */
    void updateTodoStatus(List<ImprovementMeasureDO> improvementMeasureDOList);
}

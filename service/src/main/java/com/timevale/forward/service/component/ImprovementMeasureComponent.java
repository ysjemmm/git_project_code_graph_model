package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;

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

    /**
     * 添加
     *
     * @param improvementMeasureAddReq 改进措施增加
     */
    void add(ImprovementMeasureAddReq improvementMeasureAddReq);


    /**
     * 添加待办任务
     *
     * @param improvementMeasureDO 改进测量DO
     * @return 待办Id
     */
    String addTodoTask(ImprovementMeasureDO improvementMeasureDO);
}

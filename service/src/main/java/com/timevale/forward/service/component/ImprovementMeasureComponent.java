package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureDeleteReq;

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
     * 删除
     *
     * @param id 改进措施id
     */
    void delete(Long id);

    /**
     * 添加待办任务
     *
     * @param improvementMeasureDO 改进措施DO
     * @return 待办Id
     */
    String addTodoTask(ImprovementMeasureDO improvementMeasureDO);

    /**
     * 更新待办任务
     *
     * @param improvementMeasureDO 改进措施DO
     */
    void updateTodoTask(ImprovementMeasureDO improvementMeasureDO);

    /**
     * 删除待办任务
     *
     * @param improvementMeasureDO 改进措施DO
     */
    void deleteTodoTask(ImprovementMeasureDO improvementMeasureDO);
}

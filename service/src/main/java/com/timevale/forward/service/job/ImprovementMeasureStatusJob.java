package com.timevale.forward.service.job;

import com.timevale.forward.dal.condition.ImprovementMeasureCondition;
import com.timevale.forward.dal.dao.ImprovementMeasureMapper;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.model.enums.ImprovementMeasureStatusEnum;
import com.timevale.forward.service.component.ImprovementMeasureComponent;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/19 11:05
 */
@JobHandler(value = "ImprovementMeasureStatusJob")
@Slf4j
public class ImprovementMeasureStatusJob extends IJobHandler {

    @Resource
    private ImprovementMeasureMapper improvementMeasureMapper;

    @Resource
    private ImprovementMeasureComponent improvementMeasureComponent;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        ImprovementMeasureCondition condition = ImprovementMeasureCondition.builder()
                .status(ImprovementMeasureStatusEnum.PENDING.getCode())
                .todo(true)
                .isDeleted(false)
                .build();
        List<ImprovementMeasureDO> improvementMeasureDOList = improvementMeasureMapper.selectByCondition(condition);

        improvementMeasureComponent.updateTodoStatus(improvementMeasureDOList);

        return ReturnT.SUCCESS;
    }
}

package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureModifyReq;
import com.timevale.forward.facade.api.result.ImprovementMeasureVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2022/03/17 11:24
 */
@Mapper
public interface ImprovementMeasureCopier {
    ImprovementMeasureCopier INSTANCE = Mappers.getMapper(ImprovementMeasureCopier.class);

    /**
     * 转换
     *
     * @param improvementMeasureAddReq 改进措施-新增请求
     * @return 改进措施DO
     */
    ImprovementMeasureDO convert(ImprovementMeasureAddReq improvementMeasureAddReq);

    /**
     * 转换
     *
     * @param improvementMeasureModifyReq 改进措施-修改请求
     * @return 改进措施DO
     */
    ImprovementMeasureDO convert(ImprovementMeasureModifyReq improvementMeasureModifyReq);

    /**
     * 转换
     *
     * @param improvementMeasureDO 改进措施DO
     * @return 改进措施VO
     */
    ImprovementMeasureVO convert(ImprovementMeasureDO improvementMeasureDO);


}

package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.WorkHoursRecordCondition;
import com.timevale.forward.dal.entity.WorkHoursRecordDO;
import com.timevale.forward.facade.api.query.WorkHoursRecordQueryList;
import com.timevale.forward.facade.api.request.WorkHoursRecordAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordModifyReq;
import com.timevale.forward.facade.api.result.WorkHoursRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface WorkHoursRecordCopier {

    WorkHoursRecordCopier INSTANCE = Mappers.getMapper(WorkHoursRecordCopier.class);

    /**
     * 转换转换DO
     *
     * @param workHoursRecordAddReq 对象
     * @return WorkHoursRecordDO
     */
    WorkHoursRecordDO convert(WorkHoursRecordAddReq workHoursRecordAddReq);

    /**
     * 转换转换DO
     *
     * @param workHoursRecordModifyReq 对象
     * @return WorkHoursRecordDO
     */
    WorkHoursRecordDO convert(WorkHoursRecordModifyReq workHoursRecordModifyReq);

    /**
     * 查询条件转换
     *
     * @param workHoursRecordQueryList 工时记录查询条件
     * @return 查询条件
     */
    WorkHoursRecordCondition convert(WorkHoursRecordQueryList workHoursRecordQueryList);

    /**
     * 转换转换DO
     *
     * @param workHoursRecordDO 对象
     * @return WorkHoursRecordVO
     */
    WorkHoursRecordVO convert(WorkHoursRecordDO workHoursRecordDO);

    /**
     *
     * @param workHoursRecordDO workHoursRecordDO
     * @return WorkHoursRecordVO
     */
    List<WorkHoursRecordVO> convert(List<WorkHoursRecordDO> workHoursRecordDO);

}

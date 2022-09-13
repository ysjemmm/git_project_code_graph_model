package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dto.TaskOverdueDTO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.request.TaskModifyReq;
import com.timevale.forward.facade.api.request.TaskSimpleAddReq;
import com.timevale.forward.facade.api.result.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TaskCopier {

    TaskCopier INSTANCE = Mappers.getMapper(TaskCopier.class);

    /**
     * 转换转换DO
     *
     * @param taskQueryList 对象
     * @return TaskListCondition
     */
    TaskListCondition convert(TaskQueryList taskQueryList);

    /**
     *
     * @param taskDO taskDO
     * @return TaskVO
     */
    List<TaskVO> convert(List<TaskDO> taskDO);


    /**
     * 转换转换DO
     *
     * @param taskAddReq 对象
     * @return TaskDO
     */
    TaskDO convert(TaskAddReq taskAddReq);

    /**
     * 转换转换DO
     *
     * @param taskModifyReq 对象
     * @return TaskDO
     */
    TaskDO convert(TaskModifyReq taskModifyReq);

    /**
     * 转换转换DO
     *
     * @param taskDO 对象
     * @return TaskDetailVO
     */
    TaskDetailVO convert(TaskDO taskDO);

    /**
     *
     * @param taskDO taskDO
     * @return TaskListVO
     */
    TaskListVO tansfer(TaskDO taskDO);

    /**
     *
     * @param dtoList dtoList
     * @return TaskOverdueCountVO
     */
    List<TaskOverdueCountVO> convertOverdue(List<TaskOverdueDTO> dtoList);

    /**
     *
     * @param taskSimpleAddReqs taskSimpleAddReqs
     * @return TaskDO
     */
    List<TaskDO> tansfer(List<TaskSimpleAddReq> taskSimpleAddReqs);

    /**
     *
     * @param taskSimpleAddReq taskSimpleAddReq
     * @return TaskDO
     */
    TaskDO convert(TaskSimpleAddReq taskSimpleAddReq);

    /**
     * 转换转换DO
     *
     * @param TaskDO 对象
     * @return TaskDO
     */
    HomePageSingleTaskWorkTimeVO convertT(TaskDO TaskDO);
}

package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dto.TaskBoardDTO;
import com.timevale.forward.dal.dto.TaskOverdueDTO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.request.TaskModifyReq;
import com.timevale.forward.facade.api.request.TaskSimpleAddReq;
import com.timevale.forward.facade.api.result.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
     * 里程碑转换为任务入参
     */
    @Mapping(target = "files", ignore = true)
    @Mapping(target = "alias", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "planUseTime", ignore = true)
    @Mapping(target = "actualEndDate", ignore = true)
    @Mapping(target = "actualStartDate", ignore = true)
    @Mapping(target = "productLineId", constant = "0L")
    @Mapping(target = "name", source = "milestoneName")
    @Mapping(target = "productDemandIds", expression = "java(new java.util.ArrayList<>())")
    TaskAddReq convert(ProjectMilestoneAddReq req);

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
     * 转换转换VO
     *
     * @param taskBoardDTO 对象
     * @return HomePageSingleTaskWorkTimeVO
     */
    HomePageSingleTaskWorkTimeVO convert2HomePage(TaskBoardDTO taskBoardDTO);

    /**
     * 转换转换DO
     *
     * @param TaskDO 对象
     * @return TaskDO
     */
    ProjectBoardTaskVO convert2ProjectBoard(TaskDO TaskDO);
}

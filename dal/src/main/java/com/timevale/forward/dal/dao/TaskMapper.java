package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.TaskCondition;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dto.TaskBoardDTO;
import com.timevale.forward.dal.dto.TaskOverdueDTO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.dal.entity.TaskStatusUpdateDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 11:40
 */
public interface TaskMapper {


    /**
     * 选择通过产品需求id列表
     *
     * @param condition 产品需求id列表
     * @return ProjectDO
     */
    List<TaskDO> list(TaskListCondition condition);

    /**
     * 获取任务信息
     *
     * @param ids        任务
     * @param projectIds 项目
     * @return list
     */
    List<Long> getByProjectIds(@Param("ids") List<Long> ids, @Param("projectIds") List<Long> projectIds);

    /**
     * @param ids            任务id
     * @param productLineIds 产品线id
     * @return 任务id
     */
    List<Long> getByProductLineIds(@Param("ids") List<Long> ids, @Param("productLineIds") List<Long> productLineIds);

    /**
     * 查询 by 产品需求id
     *
     * @param productDemandId 产品需求id
     */
    List<TaskDO> getByProductDemandId(@Param("productDemandId") Long productDemandId, @Param("projectId") Long projectId);

    /**
     * 通过执行人查询
     *
     * @param userIdList 用户id列表
     * @return TaskDO List
     */
    List<TaskDO> selectByExecutorList(@Param("userIdList") List<String> userIdList);

    /**
     * @param ids 任务id
     * @return 项目id
     */
    List<Long> getProjectIds(@Param("ids") List<Long> ids);


    /**
     * 新增任务信息
     *
     * @param taskDO 任务
     * @return int
     */
    int insert(TaskDO taskDO);

    /**
     * @param taskCondition 查询条件
     * @return TaskDO
     */
    TaskDO get(TaskCondition taskCondition);

    /**
     * 新增任务信息
     *
     * @param taskDO 任务
     * @return int
     */
    int update(TaskDO taskDO);

    /**
     * @param id id
     * @return TaskDO
     */
    TaskDO getById(@Param("id") Long id);

    /**
     * @param idList id 列表
     * @return TaskDO
     */
    List<TaskDO> getByIdList(@Param("idList") List<Long> idList);

    /**
     * @param projectId 项目id
     * @return 项目id
     */
    List<TaskDO> getByProjectId(@Param("projectId") Long projectId);


    /**
     * @param taskStatusUpdateDO taskStatusUpdateDO
     * @return return
     */
    int updateStatusAsProjectStatusChange(TaskStatusUpdateDO taskStatusUpdateDO);

    /**
     * 查询用户逾期任务数量、时间
     */
    List<TaskOverdueDTO> getOverdueRank(@Param("projectId") Long projectId);

    /**
     * 通过执行人查询
     *
     * @param names 用户id列表
     * @return TaskDO List
     */
    List<TaskDO> getByNameAndPid(@Param("names") List<String> names,@Param("projectId") Long projectId);

    /**
     * 查询用户逾期任务数量、时间
     */
    List<TaskBoardDTO> getByDate(@Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("accounts") Collection<String> accounts);

    /**
     *
     * @param idList idList
     * @param productLineId productLineId
     * @return int
     */
    int updateProductLineId(@Param("idList") List<Long> idList,@Param("productLineId") Long productLineId,@Param("projectId") Long projectId);

    void deleteById(@Param("id") Long id);

    void updateStage(@Param("ids") Collection<Long> ids, @Param("stage")Integer stage);
}

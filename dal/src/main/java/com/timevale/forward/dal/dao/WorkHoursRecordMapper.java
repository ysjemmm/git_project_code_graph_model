package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.WorkHoursRecordCondition;
import com.timevale.forward.dal.entity.WorkHoursRecordDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface WorkHoursRecordMapper {

    /**
     * 新增单条工时记录
     *
     * @param workHoursRecordDO 工时记录
     * @return int
     */
    int insert(WorkHoursRecordDO workHoursRecordDO);

    /**
     * 获取任务信息
     *
     * @param ids        工时
     * @param projectIds 项目
     * @return list
     */
    List<Long> getByProjectIds(@Param("ids") List<Long> ids, @Param("projectIds") List<Long> projectIds);


    /**
     * 查询
     *
     * @param condition
     * @return 工时记录信息
     */
    WorkHoursRecordDO get(WorkHoursRecordCondition condition);

    /**
     * 新增单条工时记录
     *
     * @param workHoursRecordDO 工时记录
     * @return int
     */
    int update(WorkHoursRecordDO workHoursRecordDO);

    /**
     * 根据查询条件获取工时记录列表
     *
     * @param condition 查询条件
     * @return WorkTimeRecordDO列表
     */
    List<WorkHoursRecordDO> list(WorkHoursRecordCondition condition);

    /**
     * @param id id
     * @return WorkTimeRecordDO
     */
    WorkHoursRecordDO getById(@Param("id") Long id);

    /**
     * 查询
     *
     * @param ids ids
     * @return 工时记录列表
     */
    List<WorkHoursRecordDO> getByIdList(@Param("ids") Collection<Long> ids);

    /**
     * @param ids 工时id
     * @return 项目id
     */
    List<Long> getProjectIds(@Param("ids") List<Long> ids);

    /**
     * @param id 工时id
     */
    void deleteById(@Param("id") Long id);

    /**
     * 最新工时
     * @param projectIds 项目ids
     * @param workItemType 工作项类别
     * @param workItemIds 工作项ids
     * @return 最新进度
     */
    List<WorkHoursRecordDO> getLastProgress(@Param("projectIds") List<Long> projectIds, @Param("workItemType") Integer workItemType, @Param("workItemIds") List<Long> workItemIds);

    /**
     * 时间段工时
     * @param dates 时间段
     * @return
     */
    List<WorkHoursRecordDO> getRangeWorkHours(@Param("dates") List<String> dates, @Param("projectIds") List<Long> projectIds, @Param("workItemIds") List<Long> workItemIds);

    /**
     * 根据创建人获取当日登记工时
     *
     * @param condition 查询条件
     * @return WorkTimeRecordDO列表
     */
    List<WorkHoursRecordDO> getDailyWorkingHours(WorkHoursRecordCondition condition);
}

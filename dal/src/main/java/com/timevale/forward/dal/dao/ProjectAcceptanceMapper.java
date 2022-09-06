package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProjectAcceptanceListCondition;
import com.timevale.forward.dal.entity.ProjectAcceptanceDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface ProjectAcceptanceMapper {


    /**
     * 新增
     *
     * @param projectAcceptanceDO projectAcceptanceDO
     * @return int
     */
    int insert(ProjectAcceptanceDO projectAcceptanceDO);

    /**
     * 批量新增
     *
     * @param list 列表
     * @return int
     */
    int batchInsert(@Param("list") List<ProjectAcceptanceDO> list);

    /**
     * 新增
     *
     * @param projectAcceptanceDO projectAcceptanceDO
     * @return int
     */
    int update(ProjectAcceptanceDO projectAcceptanceDO);

    /**
     * 列表
     *
     * @return TrackEventDO
     */
    List<ProjectAcceptanceDO> list(ProjectAcceptanceListCondition condition);


    /**
     * 列表
     *
     * @param id id
     * @return TrackEventDO
     */
    ProjectAcceptanceDO get(@Param("id") Long id);

}

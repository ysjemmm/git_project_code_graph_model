package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectIncomeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author jingchun
* @description 针对表【project_income(项目收益表)】的数据库操作Mapper
* @createDate 2023-02-01 14:46:42
* @Entity com.timevale.forward.dal.entity.ProjectIncomeDO
*/
public interface ProjectIncomeMapper {

    /**
     * 新增
     *
     * @param projectIncomeDO 项目收益DO
     */
    void insert(ProjectIncomeDO projectIncomeDO);

    /**
     * 更新
     *
     * @param projectIncomeDO 项目收益DO
     */
    void update(ProjectIncomeDO projectIncomeDO);

    /**
     * 删除
     *
     * @param id id
     */
    void delete(@Param("id") Long id);

    /**
     * 选择 by 项目id
     *
     * @param projectId 项目id
     */
    List<ProjectIncomeDO> selectByProjectId(@Param("projectId") Long projectId);
}





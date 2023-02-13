package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectBudgetDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author jingchun
* @description 针对表【project_budget(项目预算表)】的数据库操作Mapper
* @createDate 2023-02-01 14:46:43
* @Entity com.timevale.forward.dal.entity.ProjectBudgetDO
*/
public interface ProjectBudgetMapper {

    /**
     * 单条插入
     *
     * @param projectBudgetDO 项目预算DO
     */
    void insert(ProjectBudgetDO projectBudgetDO);

    /**
     * 批量新增
     *
     * @param list 列表
     */
    void insertBatch(@Param("list") List<ProjectBudgetDO> list);

    /**
     * 更新
     *
     * @param projectBudgetDO 项目预算DO
     */
    void update(ProjectBudgetDO projectBudgetDO);

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
    List<ProjectBudgetDO> selectByProjectId(@Param("projectId") Long projectId);


    /**
     * 选择 by 成本类型 limit 1
     *
     * @param projectId 项目id
     * @param costType  成本类型
     */
    ProjectBudgetDO selectByCostType(@Param("projectId") Long projectId, @Param("costType") String costType);
}





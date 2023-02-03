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
     * 批量新增
     *
     * @param list 列表
     */
    void insertBatch(@Param("list") List<ProjectBudgetDO> list);
}





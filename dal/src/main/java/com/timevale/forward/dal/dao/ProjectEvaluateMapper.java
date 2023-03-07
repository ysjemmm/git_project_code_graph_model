package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectEvaluateDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2023/03/06 18:30
 */
public interface ProjectEvaluateMapper {

    List<ProjectEvaluateDO> selectByProjectId(@Param("projectId") Long projectId);

}
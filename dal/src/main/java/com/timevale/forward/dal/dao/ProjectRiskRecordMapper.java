package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectRiskRecordDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/04/24 15:50
 */
public interface ProjectRiskRecordMapper {

    /**
     * 批量新增
     *
     * @param projectRiskRecordDOList 项目风险DO 列表
     */
    int batchInsert(List<ProjectRiskRecordDO> projectRiskRecordDOList);


    /**
     *
     * @param mainId mainId
     * @param type  type
     * @param date date
     * @return List
     */
    List<ProjectRiskRecordDO> get(@Param("mainId") Long mainId, @Param("type") Integer type,@Param("date") String date);

}

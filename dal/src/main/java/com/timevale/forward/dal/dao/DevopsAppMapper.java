package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.DevopsAppDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DevopsAppMapper {

    void batchInsert(@Param("list") List<DevopsAppDO> list);

    @Update("UPDATE devops_app SET stat_flag=#{statFlag}, modify_man=#{modifyMan}, modify_man_id=#{modifyManId} WHERE id=#{id}")
    void updateStatFlag(@Param("id")Long id, @Param("statFlag") Boolean statFlag);

    @Update("UPDATE devops_app SET is_deleted = true, modify_man=#{modifyMan}, modify_man_id=#{modifyManId} WHERE main_id = #{mainId} AND devops_project_sign = #{devopsProjectSign}")
    void deleteProject(@Param("mainId")Long mainId, @Param("devopsProjectSign")String devopsProjectSign);

    @Select("SELECT * FROM devops_app WHERE main_id = #{mainId} AND devops_project_sign = #{devopsProjectSign} AND is_deleted = false")
    List<DevopsAppDO> selectByProjectSign(@Param("mainId")Long mainId, @Param("devopsProjectSign")String devopsProjectSign);

    @Select("SELECT * FROM devops_app WHERE main_id = #{mainId} AND is_deleted = false")
    List<DevopsAppDO> selectByMainId(@Param("mainId")Long mainId);

    @Select("SELECT * FROM devops_app WHERE main_id = #{mainId} AND is_deleted = false GROUP BY devops_project_sign")
    List<DevopsAppDO> groupProjectByMainId(@Param("mainId")Long mainId);
}

package com.timevale.forward.dal.dao;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
* @author jingchun
* @description 针对表【bug_online_biz_demand(线上bug-业务需求关系表(N-N))】的数据库操作Mapper
* @createDate 2023-02-03 17:59:01
* @Entity com.timevale.forward.dal.entity.BugOnlineBizDemand
*/
public interface BugOnlineBizDemandMapper {

    void addRelations(@Param("bugId") Long bugId, @Param("bizDemandIds") Collection<Long> bizDemandIds);

    @Select("select biz_demand_id from bug_online_biz_demand where bug_online_id = #{bugId} and is_deleted = false")
    List<Long> getBizDemandIds(@Param("bugId") Long bugId);

    @Select("select bug_online_id from bug_online_biz_demand where bug_online_id = #{bugId} and is_deleted = false")
    List<Long> getBugOnlineIds(Long bizDemandId);
}

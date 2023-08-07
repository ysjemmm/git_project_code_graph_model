package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.DingRelationDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author jingchun
 * @description 针对表【ding_relation(对接钉钉关系表)】的数据库操作Mapper
 * @createDate 2023-08-07 14:38:23
 * @Entity generator.domain.DingRelation
 */
public interface DingRelationMapper {

    void insert(DingRelationDO entity);

    @Select("select * from ding_relation where relation_type = #{relationType} and relation_id = #{relationId} and ding_type = #{dingType}")
    List<DingRelationDO> getByRelation(@Param("relationType") Integer relationType,
                                       @Param("relationId") Long relationId,
                                       @Param("dingType") Integer dingType);

}





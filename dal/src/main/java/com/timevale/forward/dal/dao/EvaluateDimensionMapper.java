package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.EvaluateDimensionDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2023/03/06 18:30
 */
public interface EvaluateDimensionMapper {

    @Select("SELECT * FROM evaluate_dimension WHERE id = #{id} AND is_deleted = false")
    List<EvaluateDimensionDO> selectById(@Param("id")Long id);

    List<EvaluateDimensionDO> selectByIds(@Param("ids")Collection<Long> ids);

    List<EvaluateDimensionDO> getByKind(@Param("kind")Integer kind);

    List<EvaluateDimensionDO> selectByKindDate(@Param("kind")Integer kind, @Param("nowDate")Date nowDate);

}
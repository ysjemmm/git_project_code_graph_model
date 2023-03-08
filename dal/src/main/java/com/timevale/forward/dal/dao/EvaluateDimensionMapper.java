package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.EvaluateDimensionDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2023/03/06 18:30
 */
public interface EvaluateDimensionMapper {

    List<EvaluateDimensionDO> selectByIds(@Param("ids")Collection<Long> ids);

    List<EvaluateDimensionDO> selectByKind(@Param("kind")Integer kind);

    List<EvaluateDimensionDO> selectByKindDate(@Param("kind")Integer kind, @Param("nowDate")Date nowDate);

}
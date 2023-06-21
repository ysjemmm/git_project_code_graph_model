package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 11:40
 */
public interface BizChangeLogMapper {

    List<BizChangeLogDO> list(@Param("mainId") Long mainId, @Param("type") Integer type);

    Integer insert(BizChangeLogDO bizChangeLogDO);

    int batchInsert(List<BizChangeLogDO> bizChangeLogs);

    void updateIdentity(@Param("mainId") Long mainId, @Param("oldIdentity") String oldIdentity,
                        @Param("newIdentity") String newIdentity);

    void updateValue(@Param("mainId") Long mainId,
                     @Param("field") String field,
                     @Param("oldValue") String oldValue,
                     @Param("newValue") String newValue);

    List<BizChangeLogDO> listAllByActions(@Param("mainIds") Collection<Long> mainIds, @Param("type") Integer type,
                                          @Param("actions") Collection<String> actions);

    BizChangeLogDO getProjectPublishDate(@Param("mainId") Long mainId, @Param("type") Integer type, @Param("field") String field);

    @Select("SELECT * FROM biz_change_log WHERE type = #{type}")
    List<BizChangeLogDO> getByType(@Param("type")Integer type);
}
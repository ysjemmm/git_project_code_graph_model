package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import org.apache.ibatis.annotations.Param;

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
}
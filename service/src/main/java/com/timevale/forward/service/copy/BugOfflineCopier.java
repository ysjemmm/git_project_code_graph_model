package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.BugOfflineAddReq;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BugOfflineCopier {

    BugOfflineCopier INSTANCE = Mappers.getMapper(BugOfflineCopier.class);

    /**
     * 转换转换DO
     *
     * @param bugOfflineQueryListt 对象
     * @return TaskListCondition
     */
    BugOfflineListCondition convert(BugOfflineQueryList bugOfflineQueryListt);

    /**
     * 转换转换DO
     *
     * @param bugOfflineAddReq 对象
     * @return TaskDO
     */
    BugOfflineDO convert(BugOfflineAddReq bugOfflineAddReq);

}

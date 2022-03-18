package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.facade.api.result.BugOnlineDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @Date 2022/3/18 13:54
 * @Author 望轩
 */
@Mapper
public interface BugOnlineCopier {
    BugOnlineCopier INSTANCE = Mappers.getMapper(BugOnlineCopier.class);

    /**
     * BugOnlineDO --> BugOnlineDetailVO
     *
     * @param bugOnlineDO 对象
     * @return BugOfflineListCondition
     */
    BugOnlineDetailVO convert(BugOnlineDO bugOnlineDO);
}
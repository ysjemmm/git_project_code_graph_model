package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.facade.api.result.BizChangeLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2021/12/15 10:56
 */
@Mapper
public interface BizChangeLogCopier {
    BizChangeLogCopier INSTANCE = Mappers.getMapper(BizChangeLogCopier.class);

    /**
     * 转换
     *
     * @param bizChangeLogDO 业务变更日志
     * @return {@code BizChangeLogVO}
     */
    BizChangeLogVO convert(BizChangeLogDO bizChangeLogDO);

}

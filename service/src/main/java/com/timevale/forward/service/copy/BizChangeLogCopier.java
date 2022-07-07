package com.timevale.forward.service.copy;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.facade.api.result.BizChangeLogContentVO;
import com.timevale.forward.facade.api.result.BizChangeLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2021/12/15 10:56
 */
@Mapper(imports = {JSON.class, BizChangeLogContentVO.class})
public interface BizChangeLogCopier {
    BizChangeLogCopier INSTANCE = Mappers.getMapper(BizChangeLogCopier.class);

    /**
     * 转换
     *
     * @param bizChangeLogDO 业务变更日志
     * @return {@code BizChangeLogVO}
     */
    @Mapping(target = "content", expression = "java(JSON.parseObject(bizChangeLogDO.getContent(), BizChangeLogContentVO.class))")
    BizChangeLogVO convert(BizChangeLogDO bizChangeLogDO);

}

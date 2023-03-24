package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.facade.api.result.BugLogVO;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Date;

/**
 * @Date 2022/2/24 14:29
 * @Author 望轩
 */
@Mapper(
        imports = {
                Date.class,
                BugLogTypeEnum.class
        }
)
public interface BugLogCopier {
    BugLogCopier INSTANCE = Mappers.getMapper(BugLogCopier.class);

    /**
     * BugLogDO  -->  BugLogVO
     * 经办人就是创建人
     *
     * @param bugLogDO 参数
     * @return BugLogVO
     */
    @Mapping(source = "createMan", target = "operator")
    @Mapping(source = "createManId", target = "operatorId")
    @Mapping(target = "currentDate", expression = "java(new Date())")
    @Mapping(target = "typeName", expression = "java(BugLogTypeEnum.getTextByCode(bugLogVO.getType()))")
    BugLogVO convert(BugLogDO bugLogDO);


}

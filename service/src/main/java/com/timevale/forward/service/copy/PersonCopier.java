package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.PersonVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 14:25
 */
@Mapper
public interface PersonCopier {
    PersonCopier INSTANCE = Mappers.getMapper(PersonCopier.class);

    /**
     * Vo批量转换转换DO
     *
     * @param list 列表
     * @return PersonDO列表
     */
    List<PersonDO> convert(List<PersonAddReq> list);

    /**
     * DO批量转换转换VO
     *
     * @param list 列表
     * @return PersonVO列表
     */
    List<PersonVO> transform(List<PersonDO> list);

}

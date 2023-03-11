package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.model.enums.PersonLevelEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 14:25
 */
@Mapper(
        imports = {
                PersonLevelEnum.class
        }
)
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

    /**
     * PersonDO  -->  PersonVO
     *
     * @param personDO 参数
     * @return PersonVO 返回值
     */
    PersonVO change(PersonDO personDO);

    /**
     * PersonDO  -->  PersonVO
     *
     * @param personDO 参数
     * @return PersonAddReq 返回值
     */
    PersonAddReq convert(PersonDO personDO);

    /**
     * req2do
     *
     * @param req 请求
     * @return {@link PersonDO}
     */
    @Mapping(target = "personLevel", expression = "java(PersonLevelEnum.CORE.getCode())")
    PersonDO req2do(PersonAddReq req, Long mainId, Integer type);

    /**
     * req2do
     *
     * @param req 请求
     * @return {@link PersonDO}
     */
    PersonDO req2do(PersonAddReq req, Long mainId, Integer type, Integer personLevel);

    List<PersonAddReq> do2req(List<PersonDO> personDOList);

}

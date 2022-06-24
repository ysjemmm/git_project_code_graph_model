package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ColumnHideDO;
import com.timevale.forward.facade.api.request.ColumnHideModifyReq;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.forward.facade.api.result.ColumnHideVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/06/24 11:48
 */
@Mapper
public interface ColumnHideCopier {
    ColumnHideCopier INSTANCE = Mappers.getMapper(ColumnHideCopier.class);

    ColumnHideVO convert(ColumnHideDO columnHideDO);

    ColumnHideDO convert(ColumnHideModifyReq columnHideModifyReq);
}

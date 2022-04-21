package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.SearchConditionDO;
import com.timevale.forward.facade.api.request.SearchConditionAddReq;
import com.timevale.forward.facade.api.request.SearchConditionModifyReq;
import com.timevale.forward.facade.api.result.SearchConditionVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


/**
 * @author by YangXu
 * @date 2022/04/21 17:37
 */
@Mapper
public interface SearchConditionCopier {

    SearchConditionCopier INSTANCE = Mappers.getMapper(SearchConditionCopier.class);

    /**
     * 转换
     *
     * @param searchConditionAddReq 搜索条件添加请求
     */
    SearchConditionDO convert(SearchConditionAddReq searchConditionAddReq);

    /**
     * 转换
     *
     * @param searchConditionModifyReq 搜索条件修改请求
     */
    SearchConditionDO convert(SearchConditionModifyReq searchConditionModifyReq);


    /**
     * 转换
     *
     * @param searchConditionDO 搜索条件DO
     */
    SearchConditionVO convert(SearchConditionDO searchConditionDO);

}

package com.timevale.forward.service.copy;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.entity.ProductDemandGroupDO;
import com.timevale.forward.facade.api.query.ProductDemandGroupQueryList;
import com.timevale.forward.facade.api.request.ProductDemandGroupAddReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupModifyReq;
import com.timevale.forward.facade.api.result.ProductDemandGroupVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品需求分组对象转换器
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
@Mapper(
        imports = {
                JSON.class,
                CollUtil.class,
                Collectors.class
        }
)
public interface ProductDemandGroupCopier {

    ProductDemandGroupCopier INSTANCE = Mappers.getMapper(ProductDemandGroupCopier.class);

    /**
     * AddReq转DO
     * @param req 新增请求对象
     * @return DO对象
     */
    @Mapping(source = "owner.userName", target = "owner")
    @Mapping(source = "owner.userId", target = "ownerId")
    ProductDemandGroupDO toDO(ProductDemandGroupAddReq req);

    /**
     * ModifyReq转DO
     * @param req 修改请求对象
     * @return DO对象
     */
    @Mapping(source = "owner.userName", target = "owner")
        @Mapping(source = "owner.userId", target = "ownerId")
    ProductDemandGroupDO toDO(ProductDemandGroupModifyReq req);

    /**
     * DO转VO
     * @param entity DO对象
     * @return VO对象
     */
    ProductDemandGroupVO convert(ProductDemandGroupDO entity);

    /**
     * DO列表转VO列表
     * @param entityList DO列表
     * @return VO列表
     */
    List<ProductDemandGroupVO> convert(List<ProductDemandGroupDO> entityList);

    /**
     * 请求条件转换
     *
     * @param queryList 对象
     * @return ProductDemandGroupListCondition
     */
    @Mapping(target = "types", expression = "java(CollUtil.join(queryList.getTypes(),\"\"))")
    ProductDemandGroupListCondition convert(ProductDemandGroupQueryList queryList);
} 
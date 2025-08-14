package com.timevale.forward.service.copy;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.entity.ViewsDO;
import com.timevale.forward.facade.api.request.ViewsAddReq;
import com.timevale.forward.facade.api.request.ViewsModifyReq;
import com.timevale.forward.facade.api.result.ViewsVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 视图对象转换器
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
public interface ViewsCopier {

    ViewsCopier INSTANCE = Mappers.getMapper(ViewsCopier.class);

    /**
     * AddReq转DO
     * @param req 新增请求对象
     * @return DO对象
     */
    @Mapping(source = "owner.userName", target = "owner")
    @Mapping(source = "owner.userId", target = "ownerId")
    ViewsDO toDO(ViewsAddReq req);

    /**
     * ModifyReq转DO
     * @param req 修改请求对象
     * @return DO对象
     */
    ViewsDO toDO(ViewsModifyReq req);

    /**
     * DO转VO
     * @param entity DO对象
     * @return VO对象
     */
    ViewsVO convert(ViewsDO entity);

    /**
     * DO列表转VO列表
     * @param entityList DO列表
     * @return VO列表
     */
    List<ViewsVO> convert(List<ViewsDO> entityList);

} 
package com.timevale.forward.service.copy;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ViewsListCondition;
import com.timevale.forward.dal.entity.ViewsDO;
import com.timevale.forward.dal.entity.ViewsUserDO;
import com.timevale.forward.dal.entity.ViewsUserListDO;
import com.timevale.forward.facade.api.query.ViewsQueryList;
import com.timevale.forward.facade.api.request.ViewsAddReq;
import com.timevale.forward.facade.api.request.ViewsModifyReq;
import com.timevale.forward.facade.api.result.ViewsUserListVO;
import com.timevale.forward.facade.api.result.ViewsVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

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
     * DO转VO
     * @param entity DODO对象
     * @return VODO对象
     */
    ViewsUserListVO convert(ViewsUserListDO entity);

    /**
     * 请求条件转换
     *
     * @param viewsQueryList 对象
     * @return ViewsListCondition
     */
    ViewsListCondition convert(ViewsQueryList viewsQueryList);


    /**
     * DO转VO
     * @param viewsDO viewsDO对象
     * @param viewsUserDO viewsUserDO对象
     * @return VO对象
     */
    @Mapping(source = "viewsUserDO.id", target = "id")
    @Mapping(source = "viewsDO.id", target = "viewsId")
    @Mapping(source = "viewsDO.name", target = "name")
    @Mapping(source = "viewsDO.owner", target = "owner")
    @Mapping(source = "viewsDO.ownerId", target = "ownerId")
    @Mapping(source = "viewsUserDO.hidden", target = "hidden")
    @Mapping(source = "viewsUserDO.position", target = "position")
    ViewsUserListVO convert(ViewsDO viewsDO, ViewsUserDO viewsUserDO);

} 
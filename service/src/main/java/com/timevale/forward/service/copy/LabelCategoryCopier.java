package com.timevale.forward.service.copy;

import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.facade.api.query.ProductDemandLinkTrackEventQueryList;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.request.LabelCategoryAddReq;
import com.timevale.forward.facade.api.request.LabelCategoryModifyReq;
import com.timevale.forward.facade.api.result.TrackEventDetailVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
 */
@Mapper
public interface LabelCategoryCopier {

    LabelCategoryCopier INSTANCE = Mappers.getMapper(LabelCategoryCopier.class);

    /**
     *
     * @param trackEventQueryList trackEventQueryList
     * @return return
     */
    TrackEventListCondition convert(TrackEventQueryList trackEventQueryList);

    /**
     *
     * @param trackEventDO trackEventDO
     * @return return
     */
    TrackEventDetailVO convert(TrackEventDO trackEventDO);

    /**
     *
     * @param trackEventQueryList trackEventQueryList
     * @return return
     */
    TrackEventListCondition convert(ProductDemandLinkTrackEventQueryList trackEventQueryList);


    /**
     *
     * @param trackEventDOList trackEventDOList
     * @return return
     */
    List<TrackEventVO> convert(List<TrackEventDO>trackEventDOList);

    /**
     *
     * @param labelCategoryAddReq labelCategoryAddReq
     * @return LabelCategoryDO
     */
    @Mapping(source = "types", target = "type", qualifiedByName = "typeMappingStr")
    @Mapping(source = "deptIds", target = "deptId", qualifiedByName = "deptMappingStr")
    @Mapping(source = "markMans", target = "markMan", qualifiedByName = "markManMappingStr")
    @Mapping(source = "markManIds", target = "markManId", qualifiedByName = "markManIdMappingStr")
    LabelCategoryDO convert(LabelCategoryAddReq labelCategoryAddReq);

    /**
     *
     * @param labelCategoryModifyReq labelCategoryModifyReq
     * @return LabelCategoryDO
     */
    @Mapping(source = "types", target = "type", qualifiedByName = "typeMappingStr")
    @Mapping(source = "deptIds", target = "deptId", qualifiedByName = "deptMappingStr")
    @Mapping(source = "markMans", target = "markMan", qualifiedByName = "markManMappingStr")
    @Mapping(source = "markManIds", target = "markManId", qualifiedByName = "markManIdMappingStr")
    TrackEventDO convert(LabelCategoryModifyReq labelCategoryModifyReq);

    @Named("typeMappingStr")
    default String typeMappingStr(List<Integer> types){
        if(CollectionUtils.isEmpty(types)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(types);
    }

    @Named("deptMappingStr")
    default String deptMappingStr(List<Integer> deptIds){
        if(CollectionUtils.isEmpty(deptIds)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(deptIds);
    }

    @Named("markManMappingStr")
    default String markManMappingStr(List<Integer> markMans){
        if(CollectionUtils.isEmpty(markMans)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(markMans);
    }

    @Named("markManIdMappingStr")
    default String markManIdMappingStr(List<Integer> markManIds){
        if(CollectionUtils.isEmpty(markManIds)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(markManIds);
    }


}

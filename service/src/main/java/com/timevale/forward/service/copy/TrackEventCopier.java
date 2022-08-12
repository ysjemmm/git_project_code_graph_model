package com.timevale.forward.service.copy;

import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.facade.api.query.ProductDemandLinkTrackEventQueryList;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.facade.api.request.TrackEventModifyReq;
import com.timevale.forward.facade.api.result.TrackEventDetailVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.service.excel.track.TrackEvent;
import com.timevale.forward.service.excel.track.TrackFailRow;
import com.timevale.forward.service.excel.track.TrackProp;
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
public interface TrackEventCopier {

    TrackEventCopier INSTANCE = Mappers.getMapper(TrackEventCopier.class);

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

    TrackFailRow convert(TrackEvent trackEvent);

    TrackFailRow convert(TrackProp trackProp);

    /**
     *
     * @param trackEventAddReq trackEventAddReq
     * @return return
     */
    @Mapping(source = "platforms", target = "platform", qualifiedByName = "platformMappingStr")
    @Mapping(source = "envs", target = "env", qualifiedByName = "envMappingStr")
    TrackEventDO convert(TrackEventAddReq trackEventAddReq);

    @Mapping(source = "platforms", target = "platform", qualifiedByName = "platformMappingStr")
    @Mapping(source = "envs", target = "env", qualifiedByName = "envMappingStr")
    TrackEventDO convert(TrackEventModifyReq trackEventModifyReq);

    @Named("platformMappingStr")
    default String platformMappingStr(List<Integer> platforms){
        if(CollectionUtils.isEmpty(platforms)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(platforms);
    }

    @Named("envMappingStr")
    default String envMappingStr(List<Integer> envs){
        if(CollectionUtils.isEmpty(envs)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(envs);
    }

}

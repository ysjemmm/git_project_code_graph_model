package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.dal.entity.TrackPropDO;
import com.timevale.forward.facade.api.query.TrackPropQueryList;
import com.timevale.forward.facade.api.request.TrackPropAddReq;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.forward.service.excel.track.TrackEvent;
import com.timevale.forward.service.excel.track.TrackProp;
import com.timevale.forward.service.excel.track.TrackRow;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
 */
@Mapper
public interface TrackPropCopier {

    TrackPropCopier INSTANCE = Mappers.getMapper(TrackPropCopier.class);

    /**
     *
     * @param trackPropDO trackPropDO
     * @return return
     */
    List<TrackPropVO> convert(List<TrackPropDO> trackPropDO);


    /**
     *
     * @param trackPropQueryList trackPropQueryList
     * @return return
     */
    TrackPropListCondition convert(TrackPropQueryList trackPropQueryList);

    /**
     *
     * @param trackPropDO trackPropDO
     * @return return
     */
    List<TrackPropDO> change(List<TrackPropAddReq> trackPropDO);


    TrackProp convert(TrackRow trackRow);

}

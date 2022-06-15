package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.facade.api.query.ProductDemandLinkTrackEventQueryList;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.result.TrackEventVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

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

}

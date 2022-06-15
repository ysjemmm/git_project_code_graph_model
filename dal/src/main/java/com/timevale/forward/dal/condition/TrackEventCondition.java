package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class TrackEventCondition extends QueryBase {
    /**
     * id
     */
    private Long id;
    /**
     * trackMapId
     */
    private Long trackMapId;

}

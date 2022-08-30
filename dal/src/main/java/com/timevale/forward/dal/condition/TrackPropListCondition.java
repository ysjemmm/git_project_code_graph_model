package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class TrackPropListCondition extends QueryBase {

    /**
     * id
     */
    private Long id;
    /**
     * trackMapId
     */
    private Long trackMapId;
    /**
     * cnName
     */
    @WildcardEscape
    private String cnName;
    /**
     * egName
     */
    @WildcardEscape
    private String egName;
    /**
     * status
     */
    private List<Integer> status;

    /**
     * types
     */
    private List<Integer>types;

}

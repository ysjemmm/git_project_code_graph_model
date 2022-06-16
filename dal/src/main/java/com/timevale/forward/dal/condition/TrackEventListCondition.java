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
public class TrackEventListCondition extends QueryBase {

    private Long id;

    private Long trackMapId;

    @WildcardEscape
    private String cnName;

    @WildcardEscape
    private String egName;

    private List<Integer> status;

    private List<String> createManIds;

    private List<Long> filterTrackEventIds;

}

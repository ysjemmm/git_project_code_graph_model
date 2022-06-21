package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class TrackEventListCondition extends QueryBase {

    /**
     * id
     */
    private Long id;
    /**
     * trackMapIds
     */
    private List<Long> trackMapIds;
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
     * createManIds
     */
    private List<String> createManIds;
    /**
     * filterTrackEventIds
     */
    private List<Long> filterTrackEventIds;
    /**
     * trackEventIds
     */
    private List<Long> trackEventIds;

    /**
     * 创建时间
     */
    private Date createDateStart;

    /**
     * 创建时间
     */
    private Date createDateEnd;
    /**
     * 修改时间开始
     */
    private Date modifyDateStart;
    /**
     * 修改时间结束
     */
    private Date modifyDateEnd;

}

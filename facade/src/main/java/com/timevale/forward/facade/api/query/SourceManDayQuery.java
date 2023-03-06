package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.result.ToString;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Date;

/**
 * @author jingchun
 * created on 2023/3/1
 */
@Getter
@Setter
public class SourceManDayQuery extends ToString {

    // 查询开始时间
    private Date startDate;

    // 查询结束时间
    private Date endDate;

    // 工时所属人
    private Collection<String> accounts;

    // 来源id列表
    private Collection<String> sourceIds;

}

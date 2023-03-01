package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jingchun
 * created on 2023/3/1
 */
@Getter
@Setter
public class SourceManDayVO extends ToString {

    // 成员id
    private String memberId;

    // 成员花名
    private String memberName;

    // 实际人天
    private BigDecimal actualManDay;

    // 周开始日期
    private Date weekStartDate;

    // 周结束日期
    private Date weekEndDate;

}

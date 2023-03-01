package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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

}

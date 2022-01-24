package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2022/01/21 17:21
 */
@Data
public class HomePageDataIndicatorDTO {

    /**
     * 项目总数
     */
    @JSONField(name = "pj_total_q")
    private Integer projectCount;

    /**
     * 已上线项目占比
     */
    @JSONField(name = "online_pj_prop")
    private BigDecimal onlineProjectRate;

    /**
     * P0、P1项目总数
     */
    @JSONField(name = "p0p1_pj_q")
    private Integer projectCountP0P1;

    /**
     * 已上线P0、P1项目占比
     */
    @JSONField(name = "online_p0p1_pj_prop")
    private BigDecimal onlineProjectRateP0P1;

    /**
     * 逾期项目
     */
    @JSONField(name = "overdue_pj")
    private Integer overdueProjectCount;

    /**
     * 逾期项目占比
     */
    @JSONField(name = "overdue_pj_prop")
    private BigDecimal overdueProjectRate;

    /**
     * 本季度输出需求个数
     */
    @JSONField(name = "demand_total_q")
    private Integer productDemandCount;

    /**
     * 本季度需求已上线个数占比
     */
    @JSONField(name = "online_demand_prop")
    private BigDecimal onlineProductDemandRate;
}

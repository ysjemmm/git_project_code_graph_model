package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-数据指标")
public class HomePageDataIndicatorVO extends ToString {

    @ApiModelProperty("项目总数")
    private Integer projectCount;

    @ApiModelProperty("已上线项目占比")
    private String onlineProjectRate;

    @ApiModelProperty("P0P1项目总数")
    private Integer projectCountP0P1;

    @ApiModelProperty("已上线P0P1项目占比")
    private String onlineProjectRateP0P1;

    @ApiModelProperty("逾期项目")
    private Integer overdueProjectCount;

    @ApiModelProperty("逾期项目占比")
    private String overdueProjectRate;

    @ApiModelProperty("需求总数")
    private Integer productDemandCount;

    @ApiModelProperty("已上线需求占比")
    private String onlineProductDemandRate;

    @ApiModelProperty("待启动项目数")
    private Integer projectReadyStartCount;

    @ApiModelProperty("待内审项目数")
    private Integer projectReadyInternalAuditCount;

    @ApiModelProperty("待串讲项目数")
    private Integer projectReadyConstrueCount;

    @ApiModelProperty("待详设内审项目数")
    private Integer projectReadyTechnicalDetailReviewCount;

    @ApiModelProperty("待开发项目数")
    private Integer projectReadyDevelopCount;

    @ApiModelProperty("开发中项目数")
    private Integer projectDevelopingCount;

    @ApiModelProperty("待测试项目数")
    private Integer projectReadyTestCount;

    @ApiModelProperty("测试中项目数")
    private Integer projectTestingCount;

    @ApiModelProperty("待处理业务需求")
    private Integer bizDemandReadyDealWithCount;

    @ApiModelProperty("已接收待排期业务需求")
    private Integer bizDemandReadyScheduleCount;

}

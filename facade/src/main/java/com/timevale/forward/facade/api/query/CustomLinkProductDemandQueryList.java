package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 15:20
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("客户需求-产品需求查询")
public class CustomLinkProductDemandQueryList extends QueryBase {

    @ApiModelProperty("产品需求id")
    private Long productDemandId;

    @ApiModelProperty("客户需求id")
    private Long customDemandId;

    @ApiModelProperty("产品需求主题")
    private String name;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private List<Integer> priorities;

    @ApiModelProperty("业务域id")
    private List<Long> bizDomainIds;

    @ApiModelProperty("产品线id")
    private List<Long> productLineIds;

    @ApiModelProperty("产品需求负责人")
    private List<String> ownerIds;

    @ApiModelProperty("起始时间")
    private Date createDateStart;

    @ApiModelProperty("结束时间")
    private Date createDateEnd;

    @ApiModelProperty("产品需求状态")
    private List<Integer> status;
}

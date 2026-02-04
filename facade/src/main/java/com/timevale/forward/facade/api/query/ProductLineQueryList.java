package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品线查询")
public class ProductLineQueryList extends QueryBase {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("产品线负责人id")
    private List<String> ownerIds;

    @ApiModelProperty("线上bug负责人id")
    private List<String> bugOnlineOwnerIds;

    @ApiModelProperty("业务域id")
    private List<Long> bizDomainIds;

    @ApiModelProperty("sr专家id")
    private List<String> srIds;

    @ApiModelProperty("研发负责人id")
    private List<String> developmentOwnerIds;

    @ApiModelProperty("产品线等级列表 1-核心产品线; 2-即将退市产品线; 3-一般产品线")
    private List<Integer> productLineLevels;

    @ApiModelProperty("上架状态：0-未上架，1-已上架")
    private Integer listingStatus;

    @ApiModelProperty("其他负责人id")
    private List<String> otherOwnerIds;

}
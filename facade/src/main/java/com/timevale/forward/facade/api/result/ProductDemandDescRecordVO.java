package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jingchun
 * create on 2022/7/1
 */
@Getter
@Setter
@ApiModel("产品需求变更记录")
public class ProductDemandDescRecordVO extends ToString {

    @ApiModelProperty("产品需求id")
    private Long productDemandId;

    @ApiModelProperty("版本号")
    private BigDecimal version;

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("操作人id")
    private String createManId;

    @ApiModelProperty("操作人")
    private String createMan;

    @ApiModelProperty("创建时间")
    private Date createDate;

}

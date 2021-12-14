package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @Date 2021/12/14 15:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求详情")
public class BizDemandVO extends ToString {

    @ApiModelProperty("业务需求id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private Integer priority;

    @ApiModelProperty("产品线id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productLineId;

    @ApiModelProperty("业务域id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizDomainId;

    @ApiModelProperty("需求解决状态:0待评估，10已接收，20已列入项目，30项目进行中，40已完成上线，50被驳回，60已作废")
    private Integer status;
}

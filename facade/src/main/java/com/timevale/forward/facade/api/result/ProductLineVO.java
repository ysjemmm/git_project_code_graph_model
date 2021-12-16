package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2021/12/13 16:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("产品线")
public class ProductLineVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("业务域id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizDomainId;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("负责人")
    private String owner;
}

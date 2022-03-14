package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("数据订正")
public class DataModifyReq extends BaseReq {

    @ApiModelProperty(value = "订正的数据id")
    @NotNull(message = "订正的数据id不能为空")
    private List<Long> ids;

    @ApiModelProperty(value = "订正的数据类型:0项目,1产品需求,2业务需求")
    @NotNull(message = "订正的数据类型不能为空")
    private Integer type;
}

package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


/**
 * @author by YangXu
 * @date 2021/12/22 16:16
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求转交请求")
public class BizDemandTransferReq extends BaseReq{

    @ApiModelProperty("业务需求Id")
    @NotNull(message = "业务需求id不能为空")
    Long id;

    @ApiModelProperty("接收人")
    @NotBlank(message = "接收人不能为空")
    String receiveMan;

    @ApiModelProperty("接收人id")
    @NotBlank(message = "接收人id不能为空")
    String receiveManId;
}

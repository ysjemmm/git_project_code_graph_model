package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug转交")
public class BugOfflineTransferReq extends BaseReq {

    @ApiModelProperty("花名拼音")
    @NotNull(message = "花名拼音不能为空")
    private String userId;

    @ApiModelProperty("花名-真名")
    @NotNull(message = "花名-真名不能为空")
    private String userName;

    @ApiModelProperty(value = "线下bug id")
    private Long id;


}

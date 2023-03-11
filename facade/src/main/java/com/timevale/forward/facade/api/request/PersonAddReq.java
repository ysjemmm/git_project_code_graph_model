package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * @author by YangXu
 * @date 2021/12/15 14:42
 */
@EqualsAndHashCode(callSuper = false)
@Data
@ApiModel("添加相关人员")
public class PersonAddReq extends ToString {

    @ApiModelProperty("人员姓名")
    @NotBlank(message = "人员姓名不能为空")
    private String userName;

    @ApiModelProperty("人员id")
    @NotBlank(message = "人员id不能为空")
    private String userId;
}

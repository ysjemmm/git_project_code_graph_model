package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * @author by YangXu
 * @date 2021/12/15 14:42
 */
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("添加相关人员")
public class PersonAddReq extends ToString {

    @ApiModelProperty(value = "人员姓名", required = true)
    @NotBlank(message = "人员姓名不能为空")
    private String userName;

    @ApiModelProperty(value = "人员id", required = true)
    @NotBlank(message = "人员id不能为空")
    private String userId;
}

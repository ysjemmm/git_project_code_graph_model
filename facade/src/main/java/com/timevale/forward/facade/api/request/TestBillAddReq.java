package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Date 2022/1/25 11:38
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("提交提测单")
public class TestBillAddReq extends BaseReq {

    @ApiModelProperty(value = "项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty(value = "测试人")
    @NotNull(message = "测试人不能为空")
    private String testMan;

    @ApiModelProperty(value = "测试人花名拼音")
    @NotNull(message = "测试人花名拼音不能为空")
    private String testManId;
}
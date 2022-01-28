package com.timevale.forward.facade.api.result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @Date 2022/1/27 18:48
 * @Author 望轩
 */
@EqualsAndHashCode
@Data
@ApiModel("点击创建提测单返回结果对象")
public class CreateTestBillVO {
    @ApiModelProperty("计划提测时间")
    private Date planDate;

    @ApiModelProperty("该项目是否有提测单")
    private Boolean isHaveSubmitTest;

    @ApiModelProperty("提测人")
    private String submitTestMan;
}
package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @Date 2022/2/24 10:49
 * @Author 望轩
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("bug日志")
public class BugLogVO extends ToString {
    @ApiModelProperty(value = "bug日志id")
    private Integer id;

    @ApiModelProperty("主体id")
    private Integer mainId;

    @ApiModelProperty("内容变更记录类型:0线下bug,1线上bug")
    private Integer type;

    @ApiModelProperty("内容变更记录类型")
    private String typeName;

    @ApiModelProperty("变更前的值")
    private String oldValue;

    @ApiModelProperty("变更后的值")
    private String newValue;

    @ApiModelProperty("变更字段")
    private String field;

    @ApiModelProperty("按钮动作")
    private String action;

    @ApiModelProperty("单据名称")
    private String bugName;

    @ApiModelProperty("变更内容")
    private String content;

    @ApiModelProperty("经办人")
    private String operator;

    @ApiModelProperty("经办人id")
    private String operatorId;

    @ApiModelProperty("创建时间")
    private Date createDate;

}
package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;


/**
 * 业务需求变更日志VO
 *
 * @author yangxu
 * @date 2022/04/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求变更日志")
public class BizChangeLogVO extends ToString {
    @ApiModelProperty(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("主体id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long mainId;

    @ApiModelProperty("内容变更记录类型:2项目，3产品需求，4业务需求")
    private Integer type;

    @ApiModelProperty("变更前的值")
    private String oldValue;

    @ApiModelProperty("变更后的值")
    private String newValue;

    @ApiModelProperty("变更字段")
    private String field;

    @ApiModelProperty("按钮动作")
    private String action;

    @ApiModelProperty("唯一标识")
    private String identity;

    @ApiModelProperty("操作人")
    private String createMan;

    @ApiModelProperty("操作人id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;
}
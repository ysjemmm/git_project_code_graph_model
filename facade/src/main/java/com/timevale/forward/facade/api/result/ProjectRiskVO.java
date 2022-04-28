package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/13 16:38
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("项目风险VO")
public class ProjectRiskVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("风险类型 0 其它， 10 项目关键节点逾期 20 提测质量不达标 30 任务逾期")
    private Integer type;

    @ApiModelProperty("风险类型 0 其它， 10 提测质量不达标 20 任务逾期 30 项目关键节点逾期 40 项目关键节点逾期未录入")
    private String typeName;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("风险标志")
    private String sign;

    @ApiModelProperty("状态：-1 作废 0 待处理, 1 已处理")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusName;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建人id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改时间")
    private Date modifyDate;
}

package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;


/**
 * @author by YangXu
 * @date 2022/04/25 10:33
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("项目风险说明VO")
public class ProjectRiskExplanationVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("项目风险id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectRiskId;

    @ApiModelProperty("项目风险说明")
    private String explanation;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建人id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;
}

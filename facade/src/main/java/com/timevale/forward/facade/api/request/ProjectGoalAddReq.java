package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("项目目标新增")
public class ProjectGoalAddReq extends BaseReq {

    @ApiModelProperty("项目id(单独新增时传入)")
    private String projectId;

    @NotBlank(message = "项目目标名称不能为空")
    @ApiModelProperty(value = "项目目标名称", required = true)
    private String name;

    @NotNull(message = "目标性质必填")
    @Range(min = 0, max = 1, message = "目标性质只能为0:定量或者1:定性")
    @ApiModelProperty(value = "目标性质: 0定量,1定性", required = true)
    private Integer type;

    @NotNull(message = "是否为主目标必填")
    @Range(min = 0, max = 1, message = "是否为主目标只能为0:否或者1:是")
    @ApiModelProperty(value = "是否主目标: 0否,1是", required = true)
    private Integer isMain = 0;

    @NotNull(message = "目标衡量标准必填")
    @ApiModelProperty(value = "目标衡量标准", required = true)
    private String measurement;

    @ApiModelProperty("项目目标达标值")
    private BigDecimal reachValue;

    @NotNull(message = "项目目标达成日期必填")
    @ApiModelProperty(value = "项目目标达成日期", required = true)
    private Date reachDate;

}

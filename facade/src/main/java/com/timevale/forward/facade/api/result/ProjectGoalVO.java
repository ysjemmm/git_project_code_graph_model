package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目目标展示类型
 *
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("项目目标展示类")
public class ProjectGoalVO extends ToString {

    @ApiModelProperty("项目目标id")
    private Long id;

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("项目目标名称")
    private String name;

    @ApiModelProperty("目标性质: 0定量,1定性")
    private Integer type;

    @ApiModelProperty("是否主目标: 0否,1是")
    private Integer isMain = 0;

    @ApiModelProperty("目标衡量标准")
    private String measurement;

    @ApiModelProperty("项目目标达标值")
    private BigDecimal reachValue;

    @ApiModelProperty("项目目标达成日期")
    private Date reachDate;

    @ApiModelProperty("完成情况说明")
    private String completeNote;

    @ApiModelProperty("项目目标状态: 0: 进行中; 10: 已完成; 30: 未完成")
    private Integer status;

}

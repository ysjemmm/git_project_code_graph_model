package com.timevale.forward.facade.api.result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author jingchun
 * create on 2022/6/27
 */
@Getter
@Setter
@ApiModel("项目人天总类型")
public class ProjectTotalManDayVO {

    @ApiModelProperty("总人天")
    private BigDecimal projectActualManDay;

    @ApiModelProperty("项目人天列表")
    private List<ProjectManDayVO> projectManDays;

}

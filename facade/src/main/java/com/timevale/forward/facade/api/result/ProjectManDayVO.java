package com.timevale.forward.facade.api.result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 项目管理人天列表查询
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("项目人天类型")
public class ProjectManDayVO {

    @ApiModelProperty("项目成员id")
    private String memberId;

    @ApiModelProperty("项目成员名")
    private String memberName;

    @ApiModelProperty("总人天")
    private BigDecimal totalActualManDay;

    @ApiModelProperty("人天列表")
    List<ManDayVO> manDays;

    @ApiModelProperty("是否项目经理")
    private boolean isPm = false;

}

package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * @author by YangXu
 * @date 2023/02/06 15:31
 */
@Getter
@Setter
@ApiModel("内部项目详情")
public class ProjectInnerDetailVO extends ToString {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("上级项目id")
    private Long parentId;

    @ApiModelProperty("上级项目名称名称")
    private String parentProjectName;

    @ApiModelProperty("项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废")
    private Integer status;

    @ApiModelProperty("项目状态")
    private String statusName;

    @ApiModelProperty("项目经理id")
    private String pmId;

    @ApiModelProperty("项目经理")
    private String pmName;

    @ApiModelProperty("团队成员")
    private List<PersonVO> teamMember;

    @ApiModelProperty("项目计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("项目实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("项目实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("项目暂停原因")
    private String suspendReason;

    @ApiModelProperty("项目作废原因")
    private String invalidReason;
}

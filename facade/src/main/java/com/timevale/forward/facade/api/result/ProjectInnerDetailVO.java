package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

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

    @ApiModelProperty("内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目")
    private Integer innerType;

    @ApiModelProperty("内部项目类型")
    private String innerTypeName;

    @ApiModelProperty("项目经理id")
    private String pmId;

    @ApiModelProperty("项目经理")
    private String pmName;

    @ApiModelProperty("团队成员")
    private List<PersonVO> teamMember;

    @ApiModelProperty("扩展团队成员")
    private List<PersonVO> extTeamMembers;

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

    @ApiModelProperty("项目等级：0普通，1重点，2S级别，3A级别，4B级别")
    private Integer level;

    @ApiModelProperty("项目等级描述")
    private String levelName;

    @ApiModelProperty("是否是项目经理和PMO及其上级")
    private Boolean isLeaderOrPMO;
}

package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目查询")
public class ProjectQueryList extends QueryBase {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2)")
    private Byte priority;

    @ApiModelProperty("业务域")
    private List<Long> bizDomainId;

    @ApiModelProperty("产品线")
    private List<Long> productLineId;

    @ApiModelProperty("项目类型:0产品研发项目,1技术优化项目,2日常迭代")
    private Byte type;

    @ApiModelProperty("项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,50已暂停,60已作废")
    private Byte status;

    @ApiModelProperty("项目经理")
    private List<String> pm;

    @ApiModelProperty("产品经理")
    private List<String> pd;

    @ApiModelProperty("团队成员")
    private List<String> teamMember;

    @ApiModelProperty("项目计划开始时间")
    private String planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private String planEndDate;

    @ApiModelProperty("项目实际开始时间")
    private String actualStartDate;

    @ApiModelProperty("项目实际结束时间")
    private String actualEndDate;

}

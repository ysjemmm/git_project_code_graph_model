package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目列表查询")
public class ProductDemandLinkProjectQueryList extends QueryBase {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2)")
    private List<Integer> priorities;

    @ApiModelProperty("业务域")
    private List<Long> bizDomainIds;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("项目类型:0产品研发项目,1技术优化项目,2日常迭代")
    private List<Integer> types;

    @ApiModelProperty("项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,50已暂停,60已作废")
    private List<Integer> status;

    @ApiModelProperty("项目经理")
    private List<String> pms;

    @ApiModelProperty("产品经理")
    private List<String> pds;

    @ApiModelProperty("团队成员")
    private List<String> teamMembers;

    @ApiModelProperty("项目计划开始时间左区间")
    private Date planStartDateLeft;

    @ApiModelProperty("项目计划开始时间右区间")
    private Date planStartDateRight;

    @ApiModelProperty("项目计划结束时间左区间")
    private Date planEndDateLeft;

    @ApiModelProperty("项目计划结束时间右区间")
    private Date planEndDateRight;

    @ApiModelProperty("项目实际开始时间左区间")
    private Date actualStartDateLeft;

    @ApiModelProperty("项目实际开始时间右区间")
    private Date actualStartDateRight;

    @ApiModelProperty("项目实际结束时间左区间")
    private Date actualEndDateLeft;

    @ApiModelProperty("项目实际结束时间右区间")
    private Date actualEndDateRight;

    @ApiModelProperty("产品需求id")
    @NotNull(message = "产品需求id不能为空")
    private Long productDemandId;

}

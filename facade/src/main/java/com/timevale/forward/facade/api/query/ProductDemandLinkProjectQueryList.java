package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求-项目列表查询")
public class ProductDemandLinkProjectQueryList extends QueryBase {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("项目类型 0-产研项目 1-内部项目")
    private Integer category = 0;

    @ApiModelProperty("优先级:0(P0),10(P1),20(P2),30(P3)")
    private List<Integer> priorities;

    @ApiModelProperty("业务域")
    private List<Long> bizDomainIds;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("项目性质:0产品研发项目,1技术优化项目,2日常迭代")
    private List<Integer> types;

    @ApiModelProperty("项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废")
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

    @ApiModelProperty("产品需求id")
    private Long productDemandId;

    @ApiModelProperty("是否包含标签")
    private Boolean containLabel = true;

    @ApiModelProperty("选中为标签时填写,标签id")
    private List<Long> labelIds;

    @ApiModelProperty("选中为标签类别时填写,类别id")
    private List<Long> labelCategoryIds;

    @ApiModelProperty("项目类型:0-空，1-PBG项目/基线项目，2-PBG项目/1-N客开项目，3-职能后台项目/流程IT中心项目")
    private Integer kind;

    @ApiModelProperty("是否客开需求")
    private Integer customerDev;

    @ApiModelProperty("来源id")
    private String sourceId;

    @ApiModelProperty("srId列表")
    private List<String> srs;

    @ApiModelProperty("是否包含关联分组, true: 搜索未关联分组的项目，false: 所有")
    private Boolean unLinkGroup;
}

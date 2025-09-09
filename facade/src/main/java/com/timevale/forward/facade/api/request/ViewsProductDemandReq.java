package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author qiyuan
 * create on 2025/7/14
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图产品需求查询条件")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ViewsProductDemandReq extends ViewsFilterReq {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    private List<Integer> priorities;

    @ApiModelProperty("业务域")
    private List<Long> bizDomainIds;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("子产品线id")
    private List<Long> subProductLineIds;

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,-10已暂停,-20已作废")
    private List<Integer> status;

    @ApiModelProperty("负责人")
    private List<String> ownerIds;

    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;

    @ApiModelProperty("起始时间")
    private Date createDateStart;

    @ApiModelProperty("结束时间")
    private Date createDateEnd;

    @ApiModelProperty("是否包含标签")
    private Boolean containLabel = true;

    @ApiModelProperty("选中为标签时填写,标签id")
    private List<Long> labelIds;

    @ApiModelProperty("选中为标签类别时填写,类别id")
    private List<Long> labelCategoryIds;

    @ApiModelProperty("产品需求类型")
    private List<Integer> types;

    @ApiModelProperty("预期排期时间-起始时间")
    private Date expectScheduleTimeStart;

    @ApiModelProperty("预期排期时间-结束时间")
    private Date expectScheduleTimeEnd;

    @ApiModelProperty("当前页")
    private Integer pageNum;

    @ApiModelProperty("页大小")
    private Integer pageSize;

    @ApiModelProperty("目标客户/用户/项目")
    private String targetCustomer;

    @ApiModelProperty("客户等级")
    private String customerGrade;

    @ApiModelProperty("标签映射")
    private Object labelNames;

    @ApiModelProperty("标签内容")
    private String contentValue;
}

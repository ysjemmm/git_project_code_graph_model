package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author qiyuan
 * create on 2025/7/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图业务需求查询条件")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ViewsBizDemandReq extends ViewsFilterReq {

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("业务需求id")
    private Long id;

    @ApiModelProperty("目标客户/用户/项目")
    private String targetCustomer;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private List<Integer> priorityList;

    @ApiModelProperty("业务域id")
    private List<Long> bizDomainIdList;

    @ApiModelProperty("产品线id")
    private List<Long> productLineIdList;

    @ApiModelProperty("子产品线id")
    private List<Long> subProductLineIdList;

    @ApiModelProperty("起始时间")
    private Date createDateStart;

    @ApiModelProperty("结束时间")
    private Date createDateEnd;

    @ApiModelProperty("预计上线时间0 (Q1上旬)，1(Q1中旬)，2 (Q1下旬)，3 (Q2上旬)，4 (Q2中旬)，5(Q2下旬)，6(Q3上旬)，7 (Q3中旬)，8 (Q3下旬)，6 (Q4上旬)，7 (Q4中旬)，8 (Q4下旬)，9暂无法评估\n")
    private List<Integer> planReleaseDateList;

    @ApiModelProperty("需求解决状态")
    private List<Integer> statusList;

    @ApiModelProperty("需求提交人id 列表")
    private List<String> submitManIdList;

    @ApiModelProperty("需求接收人id 列表")
    private List<String> receiveManIdList;

    @ApiModelProperty("需求部门id")
    private List<Long> deptIdList;
    
    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;

    @ApiModelProperty("项目发布时间-起始时间")
    private Date projectEndDateStart;

    @ApiModelProperty("项目发布时间-结束时间")
    private Date projectEndDateEnd;

    @ApiModelProperty("排序字段")
    private String orderFiled;

    @ApiModelProperty("排序规则：0正序，1逆序")
    private Integer orderCollation;

    @ApiModelProperty("来源id")
    private String sourceId;

    @ApiModelProperty("是否包含标签")
    private Boolean containLabel = true;

    @ApiModelProperty("选中为标签时填写,标签id")
    private List<Long> labelIds;

    @ApiModelProperty("选中为标签类别时填写,类别id")
    private List<Long> labelCategoryIds;

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("期望上线时间开始")
    private Date hopeReleaseDayStart;

    @ApiModelProperty("期望上线时间结束")
    private Date hopeReleaseDayEnd;

    @ApiModelProperty("是否为客户开发项目")
    private Boolean customerDevDemand;

    @ApiModelProperty("产品方案")
    private String productSolution;

    @ApiModelProperty("查询来源: 0-默认产研系统 1-交付项目")
    private int querySource;

    @ApiModelProperty("SR专家id集合")
    private Collection<String> srExpertIds;

    @ApiModelProperty("是否影响客户订单")
    private Boolean affectCustomerOrder;

    @ApiModelProperty("卡单说明")
    private String stuckOrderInstructions;

    @ApiModelProperty("客户等级")
    private String customerGrade;
}

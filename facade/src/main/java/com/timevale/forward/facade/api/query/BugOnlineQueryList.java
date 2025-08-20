package com.timevale.forward.facade.api.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/17 14:20
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug列表查询")
public class BugOnlineQueryList extends QueryBase {
    @ApiModelProperty("bug标题")
    private String name;

    @ApiModelProperty("bug状态")
    private List<Integer> status;

    @ApiModelProperty("经办人id")
    private List<String> operatorIdList;

    @ApiModelProperty("历史经办人列表，名称为花名-真名")
    private List<String> historyOperators;

    @ApiModelProperty("提出人id")
    private List<String> proposerIdList;

    @ApiModelProperty("所属业务域")
    private List<Long> bizDomainIdList;

    @ApiModelProperty("所属产品线")
    private List<Long> productLineIdList;

    @ApiModelProperty("bug优先级列表")
    private List<Integer> priorities;

    @ApiModelProperty("子bug优先级列表")
    private List<Integer> subPriorities;

    @ApiModelProperty("bug环境")
    private List<Integer> envs;

    @ApiModelProperty("bug原因")
    private List<Integer> reasons;

    @ApiModelProperty("不用修复原因")
    private List<Integer> dismissCauseList;

    @ApiModelProperty("bug所属端")
    private List<Integer> belongs;

    @ApiModelProperty("创建时间左区间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date createDateLeft;

    @ApiModelProperty("创建时间右区间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date createDateRight;

    @ApiModelProperty("更新时间左区间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date modifyDateLeft;

    @ApiModelProperty("更新时间右区间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date modifyDateRight;

    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("来源列表: forward 产研系统， support 运营支撑平台， duty 值班反馈")
    private List<String> sourceList;

    @ApiModelProperty("排序字段")
    private String orderFiled;

    @ApiModelProperty("排序规则：0正序，1逆序")
    private Integer orderCollation;

    @ApiModelProperty("是否包含标签")
    private Boolean containLabel = true;

    @ApiModelProperty("选中为标签时填写,标签id")
    private List<Long> labelIds;

    @ApiModelProperty("选中为标签类别时填写,类别id")
    private List<Long> labelCategoryIds;

    @ApiModelProperty("模块id")
    private List<Long> modelIds;

    @ApiModelProperty("来源id")
    private String sourceId;

    @ApiModelProperty("0华南大区，1华北大区，2华东大区，3西部大区，9其他大区")
    private List<Integer> areas;

    @ApiModelProperty("是否是系统关闭bug")
    private Boolean isSystemCloseBug;

    @ApiModelProperty("详情描述")
    private String describe;

    @ApiModelProperty("bug原因归因列表")
    private List<Integer> reasonStageList;

    @ApiModelProperty("不用修复原因归因列表")
    private List<Integer> dismissCauseStageList;

    @ApiModelProperty("客户等级")
    private List<String> customerGradeList;

    @ApiModelProperty("bug问题类型")
    private List<Integer> categoryList;

    @ApiModelProperty("重复打开次数判断类型")
    private Integer openCountType;

    @ApiModelProperty("重复打开次数")
    private Integer openCount;

    @ApiModelProperty("我是当下经办人")
    private Boolean currentOperatorOnly = true;

}
package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 视图线下bug查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图线下bug查询条件")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ViewsBugOfflineReq extends ViewsFilterReq {

    @ApiModelProperty("bug标题")
    private String name;

    @ApiModelProperty("bug状态")
    private List<Integer> status;

    @ApiModelProperty("经办人id")
    private List<String> operatorIds;

    @ApiModelProperty("提出人")
    private List<String> proposerIds;

    @ApiModelProperty("关联项目")
    private List<Long> projectIds;

    @ApiModelProperty("所属业务域")
    private List<Long> bizDomainIds;

    @ApiModelProperty("所属产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("bug优先级")
    private List<Integer> priorities;

    @ApiModelProperty("严重程度")
    private List<Integer> severities;

    @ApiModelProperty("bug环境")
    private List<Integer> envs;

    @ApiModelProperty("bug原因")
    private List<String> reasons;

    @ApiModelProperty("bug来源")
    private List<Integer> sources;

    @ApiModelProperty("bug所属端")
    private List<Integer> belongs;

    @ApiModelProperty("不用修复原因")
    private List<Integer> unhandleReasons;

    @ApiModelProperty("历史经办人列表")
    private List<String> historyOperators;

    @ApiModelProperty("创建时间左区间")
    private Date createDateLeft;

    @ApiModelProperty("创建时间右区间")
    private Date createDateRight;

    @ApiModelProperty("修改时间左区间")
    private Date modifyDateLeft;

    @ApiModelProperty("修改时间右区间")
    private Date modifyDateRight;

    @ApiModelProperty("打回次数判断类型")
    private Integer returnCountType;

    @ApiModelProperty("打回次数")
    private Integer returnCount;

    @ApiModelProperty("重复打开次数判断类型")
    private Integer openCountType;

    @ApiModelProperty("重复打开次数")
    private Integer openCount;

    @ApiModelProperty("CURRENT_USER:我的,RECEIVE:我接收的,TEAM:我团队的,COPIER:抄送我的,ALL:全部")
    private String ascription;

    @ApiModelProperty("是否包含标签")
    private Boolean containLabel = true;

    @ApiModelProperty("选中为标签时填写,标签id")
    private List<Long> labelIds;

    @ApiModelProperty("选中为标签类别时填写,类别id")
    private List<Long> labelCategoryIds;

    @ApiModelProperty("我是当下经办人")
    private Boolean currentOperatorOnly = true;

    @ApiModelProperty("当前页")
    private Integer pageNum;

    @ApiModelProperty("页大小")
    private Integer pageSize;
}

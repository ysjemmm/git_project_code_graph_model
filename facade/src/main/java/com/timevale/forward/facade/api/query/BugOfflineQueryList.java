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
@ApiModel("线下bug列表查询")
public class BugOfflineQueryList extends QueryBase {

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

    @ApiModelProperty("bug环境")
    private List<Integer> envs;

    @ApiModelProperty("bug原因")
    private List<String> reasons;

    @ApiModelProperty("bug来源")
    private List<Integer> sources;

    @ApiModelProperty("bug所属端")
    private List<Integer>belongs;

    @ApiModelProperty("创建时间左区间")
    private Date createDateLeft;

    @ApiModelProperty("创建时间右区间")
    private Date createDateRight;

    @ApiModelProperty("修改时间左区间")
    private Date modifyDateLeft;

    @ApiModelProperty("修改时间有区间")
    private Date modifyDateRight;

    @ApiModelProperty("打回次数判断类型")
    private Integer returnCountType;

    @ApiModelProperty("打回次数")
    private Integer returnCount;

    @ApiModelProperty("重复打开次数判断类型")
    private Integer openCountType;

    @ApiModelProperty("重复打开次数")
    private Integer openCount;

    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;

    @ApiModelProperty("排序字段")
    private String orderFiled;

    @ApiModelProperty("排序规则：0正序，1逆序")
    private Integer orderCollation;
}

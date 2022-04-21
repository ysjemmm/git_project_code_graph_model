package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("查询条件")
public class SearchConditionVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("所属模块:0 业务需求，10 产品需求， 20 项目管理， 30 任务管理， 40 线下bug， 50 线上bug， 60 故障单")
    private Integer model;

    @ApiModelProperty("tab：0 全部， 10 我的， 20 我接收的， 30 抄送我的， 40 我下属的， 50 我团队的， 60 我团结提交的， 70 我团队接收的， 80 我部门的")
    private Integer tabType;

    @ApiModelProperty("条件名称")
    private String name;

    @ApiModelProperty("内容")
    private String content;
}

package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.forward.facade.api.request.ViewsFilterReq;
import com.timevale.forward.facade.api.request.ViewsGroupFieldReq;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 视图用户列表VO
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图用户列表")
public class ViewsUserListVO extends ToString {

    @ApiModelProperty("主键id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("视图id")
    private Long viewsId;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("负责人id")
    private String ownerId;

    @ApiModelProperty("0-显示，1-隐藏")
    private Boolean hidden;

    @ApiModelProperty("相对位置")
    private BigDecimal position;

    @ApiModelProperty("共享使用者")
    private List<String> shareUsers;

    @ApiModelProperty("视图名称")
    private String name;

    @ApiModelProperty("分组字段")
    private List<ViewsGroupFieldReq> groupFields;

    @ApiModelProperty("筛选条件")
    private ViewsFilterReq filters;


} 